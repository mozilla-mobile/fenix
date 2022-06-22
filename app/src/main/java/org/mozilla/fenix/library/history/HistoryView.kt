/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.library.history

import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.MotionEvent.ACTION_UP
import android.view.ViewGroup
import androidx.core.view.GestureDetectorCompat
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.paging.LoadState
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import mozilla.components.support.base.feature.UserInteractionHandler
import org.mozilla.fenix.FeatureFlags
import org.mozilla.fenix.R
import org.mozilla.fenix.databinding.ComponentHistoryBinding
import org.mozilla.fenix.ext.components
import org.mozilla.fenix.library.LibraryPageView
import org.mozilla.fenix.theme.ThemeManager


/**
 * View that contains and configures the History List
 */
class HistoryView(
    container: ViewGroup,
    val interactor: HistoryInteractor,
    val onZeroItemsLoaded: () -> Unit,
    val onEmptyStateChanged: (Boolean) -> Unit,
    val invalidateHistoryDataSource: () -> Unit,
    val isSyncedHistory: Boolean
) : LibraryPageView(container), UserInteractionHandler  {

    val binding = ComponentHistoryBinding.inflate(
        LayoutInflater.from(container.context), container, true
    )

    var mode: HistoryFragmentState.Mode = HistoryFragmentState.Mode.Normal
        private set

    private var stickyHeaderClickDetector: GestureDetectorCompat

    val historyAdapter = HistoryAdapter(interactor) { isEmpty ->
        onEmptyStateChanged(isEmpty)
    }.apply {
        addLoadStateListener {
            // First call will always have itemCount == 0, but we want to keep adapterItemCount
            // as null until we can distinguish an empty list from populated, so updateEmptyState()
            // could work correctly.
            if (itemCount > 0) {
                adapterItemCount = itemCount
            } else if (it.source.refresh is LoadState.NotLoading &&
                it.append.endOfPaginationReached &&
                itemCount < 1
            ) {
                adapterItemCount = 0
                onZeroItemsLoaded.invoke()
            }
        }
    }
    private val layoutManager = LinearLayoutManager(container.context)
    private var adapterItemCount: Int? = null
    private val decorator = StickyHeaderDecoration(historyAdapter)

    private fun getStickyHeaderBottom() : Float {
        return decorator.getStickyHeaderBottom()
    }

    init {
        stickyHeaderClickDetector = GestureDetectorCompat(activity, StickyHeaderGestureListener(
            recyclerView = binding.historyList,
            onStickyHeaderClicked = ::onStickyHeaderClicked,
            stickyHeaderHeight = ::getStickyHeaderBottom,
        ))

        binding.historyList.apply {
            layoutManager = this@HistoryView.layoutManager
            adapter = historyAdapter
            (itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false

            // Adding sticky header and listener to intercept clicks on it. Sticky header is drawn
            // on Canvas over recyclerview, it is not a regular view and regular click listener
            // can not be used.
            addItemDecoration(decorator)
            addOnItemTouchListener(object : RecyclerView.SimpleOnItemTouchListener() {
                override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
                    val consumed = stickyHeaderClickDetector.onTouchEvent(e)
                    return if (e.action == ACTION_UP) {
                        consumed
                    } else {
                        false
                    }
                }
            })
        }

        val primaryTextColor = ThemeManager.resolveAttribute(R.attr.textPrimary, context)
        binding.swipeRefresh.setColorSchemeColors(primaryTextColor)
        binding.swipeRefresh.setOnRefreshListener {
            interactor.onRequestSync()
        }
    }

    private fun onStickyHeaderClicked(position: Int) {
        // A click could happen while adapter is recalculating item positions, like during pull
        // to refresh, so we have to make sure that there is an item to scroll to.
        if (historyAdapter.itemCount > position) {
            layoutManager.smoothScrollToPosition(
                binding.historyList,
                RecyclerView.State(),
                historyAdapter.getHeaderPositionForItem(position)
            )
        }
    }

    fun update(state: HistoryFragmentState) {
        val oldMode = mode

        binding.progressBar.isVisible = state.isDeletingItems
        binding.swipeRefresh.isRefreshing = state.mode === HistoryFragmentState.Mode.Syncing
        binding.swipeRefresh.isEnabled = state.mode === HistoryFragmentState.Mode.Normal ||
                state.mode === HistoryFragmentState.Mode.Syncing

        mode = state.mode

        historyAdapter.updatePendingDeletionItems(state.pendingDeletionItems)

        historyAdapter.updateMode(state.mode)

        // We want to update the one item above the upper border of the screen, because
        // RecyclerView won't redraw it on scroll and onBindViewHolder() method won't be called.
        val first = layoutManager.findFirstVisibleItemPosition() - 1
        val last = layoutManager.findLastVisibleItemPosition() + 4
//        Log.d("CollapseDebugging", "first = $first, last = $last")
        historyAdapter.notifyItemRangeChanged(first, last - first)

//        if (headerStateChanged) {
//            historyAdapter.notifyDataSetChanged()
//        } else {
//            // We want to update the one item above the upper border of the screen, because
//            // RecyclerView won't redraw it on scroll and onBindViewHolder() method won't be called.
//            val first = layoutManager.findFirstVisibleItemPosition() - 1
//            val last = layoutManager.findLastVisibleItemPosition() + 1
//            historyAdapter.notifyItemRangeChanged(first, last - first)
//        }

        if (state.mode::class != oldMode::class) {
            interactor.onModeSwitched()
        }

        when (val mode = state.mode) {
            is HistoryFragmentState.Mode.Normal -> {
                val title = if (isSyncedHistory) {
                    context.getString(R.string.history_from_other_devices)
                } else {
                    context.getString(R.string.library_history)
                }
                setUiForNormalMode(title)
            }
            is HistoryFragmentState.Mode.Editing -> {
                setUiForSelectingMode(
                    context.getString(R.string.history_multi_select_title, mode.selectedItems.size)
                )
            }
            else -> {
                // no-op
            }
        }
    }

    override fun onBackPressed(): Boolean {
        return interactor.onBackPressed()
    }
}
