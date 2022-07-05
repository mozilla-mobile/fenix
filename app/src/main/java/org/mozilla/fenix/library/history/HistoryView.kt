/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.library.history

import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.MotionEvent.ACTION_UP
import android.view.ViewGroup
import androidx.core.view.GestureDetectorCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import mozilla.components.support.base.feature.UserInteractionHandler
import org.mozilla.fenix.R
import org.mozilla.fenix.databinding.ComponentHistoryBinding
import org.mozilla.fenix.library.LibraryPageView
import org.mozilla.fenix.theme.ThemeManager

/**
 * View that contains and configures the History List
 */
class HistoryView(
    container: ViewGroup,
    val interactor: HistoryInteractor,
    val historyViewItemDataSource: HistoryViewItemDataSource,
    val onEmptyStateChanged: (Boolean) -> Unit,
    val isSyncedHistory: Boolean
) : LibraryPageView(container), UserInteractionHandler {

    val binding = ComponentHistoryBinding.inflate(
        LayoutInflater.from(container.context), container, true
    )

    var mode: HistoryFragmentState.Mode = HistoryFragmentState.Mode.Normal
        private set

    val historyAdapter = HistoryAdapter(interactor) { isEmpty ->
        onEmptyStateChanged(isEmpty)
    }
    private val layoutManager = LinearLayoutManager(container.context)
    private val decorator = StickyHeaderDecoration(historyAdapter)
    private var stickyHeaderClickDetector: GestureDetectorCompat

    init {
        stickyHeaderClickDetector = GestureDetectorCompat(
            activity,
            StickyHeaderGestureListener(
                recyclerView = binding.historyList,
                onStickyHeaderClicked = ::onStickyHeaderClicked,
                stickyHeaderBottom = ::getStickyHeaderBottom,
            )
        )

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

    fun update(state: HistoryFragmentState) {
        val oldMode = mode

        binding.progressBar.isVisible = state.isDeletingItems
        binding.swipeRefresh.isRefreshing = state.mode === HistoryFragmentState.Mode.Syncing
        binding.swipeRefresh.isEnabled = state.mode === HistoryFragmentState.Mode.Normal ||
            state.mode === HistoryFragmentState.Mode.Syncing
        mode = state.mode

        historyAdapter.updatePendingDeletionItems(state.pendingDeletionItems)
        historyAdapter.updateMode(state.mode)

        // We track header positions in the list inside the onBindViewHolder method. Rebinding is
        // necessary for that to work.
        val first = layoutManager.findFirstVisibleItemPosition() - 1
        val last = layoutManager.findLastVisibleItemPosition() + 1
        historyAdapter.notifyItemRangeChanged(first, last - first)

        if (state.mode::class != oldMode::class) {
            interactor.onModeSwitched()
        }

        historyViewItemDataSource.setCollapsedHeaders(state.collapsedHeaders)
        historyViewItemDataSource.setDeleteItems(state.pendingDeletionItems, state.hiddenHeaders)
        historyViewItemDataSource.setEmptyState(state.isEmpty)

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

    private fun onStickyHeaderClicked(position: Int) {
        layoutManager.smoothScrollToPosition(
            binding.historyList,
            RecyclerView.State(),
            historyAdapter.getHeaderPositionForItem(position)
        )
    }

    private fun getStickyHeaderBottom(): Float {
        return decorator.getStickyHeaderBottom()
    }

    override fun onBackPressed(): Boolean {
        return interactor.onBackPressed()
    }
}
