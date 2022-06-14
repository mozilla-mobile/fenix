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
) : LibraryPageView(container), UserInteractionHandler, RecyclerView.OnItemTouchListener {

    val binding = ComponentHistoryBinding.inflate(
        LayoutInflater.from(container.context), container, true
    )

    var mode: HistoryFragmentState.Mode = HistoryFragmentState.Mode.Normal
        private set

    private var collapsedHeaders: Set<HistoryItemTimeGroup> = setOf()
    private lateinit var detector: GestureDetectorCompat

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
        detector = GestureDetectorCompat(activity, StickyHeaderGestureListener(
            stickyHeaderHeight = ::getStickyHeaderBottom,
            binding.historyList,
            ::onStickyHeaderClicked
        ))

        binding.historyList.apply {
            layoutManager = this@HistoryView.layoutManager
            adapter = historyAdapter
            (itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false
            addItemDecoration(decorator)
            addOnItemTouchListener(this@HistoryView)
        }

        val primaryTextColor = ThemeManager.resolveAttribute(R.attr.textPrimary, context)
        binding.swipeRefresh.setColorSchemeColors(primaryTextColor)
        binding.swipeRefresh.setOnRefreshListener {
            interactor.onRequestSync()
        }
    }

    private fun onStickyHeaderClicked(position: Int) {
        Log.d("onStickyHeaderClicked", "position = $position")
//        historyAdapter.onStickyHeaderClicked(position)
        layoutManager.smoothScrollToPosition(
            binding.historyList,
            RecyclerView.State(),
            historyAdapter.getHeaderPositionForItem(position)
        )
    }

    fun update(state: HistoryFragmentState) {
        val oldMode = mode
        val headerStateChanged = collapsedHeaders != state.collapsedHeaders
        if (headerStateChanged) {
//            invalidateHistoryDataSource.invoke()
            historyAdapter.collapsedHeaders = state.collapsedHeaders
            collapsedHeaders = state.collapsedHeaders
        }

        binding.progressBar.isVisible = state.isDeletingItems
        binding.swipeRefresh.isRefreshing = state.mode === HistoryFragmentState.Mode.Syncing
        binding.swipeRefresh.isEnabled =
            state.mode === HistoryFragmentState.Mode.Normal || state.mode === HistoryFragmentState.Mode.Syncing
        mode = state.mode

        historyAdapter.updatePendingDeletionItems(state.pendingDeletionItems)

        updateEmptyState(userHasHistory = !state.isEmpty)

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

//                setUiForNormalMode(
//                    context.getString(R.string.library_history)
//                )
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

    private fun updateEmptyState(userHasHistory: Boolean) {
        binding.historyList.isInvisible = !userHasHistory
        binding.historyEmptyView.isVisible = !userHasHistory
        binding.topSpacer.isVisible = !userHasHistory

        with(binding.recentlyClosedNavEmpty) {
            recentlyClosedNav.setOnClickListener {
                interactor.onRecentlyClosedClicked()
            }
            val numRecentTabs = recentlyClosedNav.context.components.core.store.state.closedTabs.size
            recentlyClosedTabsDescription.text = String.format(
                context.getString(
                    if (numRecentTabs == 1) {
                        R.string.recently_closed_tab
                    } else {
                        R.string.recently_closed_tabs
                    }
                ),
                numRecentTabs
            )
            recentlyClosedNav.isVisible = !userHasHistory
        }

        with(binding.syncedHistoryNavEmpty) {
            syncedHistoryNav.setOnClickListener {
                interactor.onSyncedHistoryClicked()
            }
            syncedHistoryNav.isVisible = FeatureFlags.showSyncedHistory && !userHasHistory
        }

        if (!userHasHistory) {
            binding.historyEmptyView.announceForAccessibility(context.getString(R.string.history_empty_message))
        }
    }

    override fun onBackPressed(): Boolean {
        return interactor.onBackPressed()
    }

    override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
        val consumed = detector.onTouchEvent(e)
//        Log.d("HistoryView", "consumed = $consumed, event = $e")
        return if (e.action == ACTION_UP) {
            consumed
        } else {
            false
        }
//        return detector.onTouchEvent(e)
    }

    override fun onTouchEvent(rv: RecyclerView, e: MotionEvent) {
//        detector.onTouchEvent(e)
    }

    override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {
//        Log.d("kolobok", "disallowIntercept = $disallowIntercept")
    }
}
