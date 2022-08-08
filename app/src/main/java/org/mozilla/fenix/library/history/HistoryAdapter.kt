/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.library.history

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import org.mozilla.fenix.library.history.viewholders.EmptyViewHolder
import org.mozilla.fenix.library.history.viewholders.HistoryGroupViewHolder
import org.mozilla.fenix.library.history.viewholders.HistoryViewHolder
import org.mozilla.fenix.library.history.viewholders.RecentlyClosedViewHolder
import org.mozilla.fenix.library.history.viewholders.SignInViewHolder
import org.mozilla.fenix.library.history.viewholders.SyncedHistoryViewHolder
import org.mozilla.fenix.library.history.viewholders.TimeGroupSeparatorViewHolder
import org.mozilla.fenix.library.history.viewholders.TimeGroupViewHolder
import org.mozilla.fenix.library.history.viewholders.TopSeparatorViewHolder
import org.mozilla.fenix.selection.SelectionHolder

/**
 * Adapter for the list of visited pages, that uses Paging 3 versions of the Paging library.
 */
class HistoryAdapter(
    private val historyInteractor: HistoryInteractor,
    private val onEmptyStateChanged: (Boolean) -> Unit,
) : PagingDataAdapter<HistoryViewItem, RecyclerView.ViewHolder>(historyDiffCallback),
    SelectionHolder<History> {

    private var mode: HistoryFragmentState.Mode = HistoryFragmentState.Mode.Normal
    private var pendingDeletionItems = emptySet<PendingDeletionHistory>()
    private var recycler: RecyclerView? = null

    override val selectedItems
        get() = mode.selectedItems

    init {
        //  Tracking updates of data flow. Delete data flow might have filtered out the last history
        //  or group history item, so the empty view should be displayed. Listener is triggered on
        //  every change, including loading additional items, so we don't want to do extra checks
        //  after there are more items in the adapter than in a single load.
        addOnPagesUpdatedListener {
            if (itemCount <= HistoryViewItemFlow.PAGE_SIZE) {
                for (i in 0 until itemCount) {
                    val item = getItem(i)
                    // If there is a single visible item, it's enough to change the empty state of the view.
                    val hasVisibleItems = item is HistoryViewItem.HistoryItem ||
                        item is HistoryViewItem.HistoryGroupItem ||
                        item is HistoryViewItem.TimeGroupHeader ||
                        item is HistoryViewItem.SignInHistoryItem
                    if (hasVisibleItems) {
                        onEmptyStateChanged.invoke(false)
                        break
                    } else if (i + 1 == itemCount) {
                        // If we reached the bottom of the list and there still has been zero visible items,
                        // we can can change the History view state to empty.
                        onEmptyStateChanged.invoke(true)
                    }
                }
            }
        }
    }

    override fun getItemViewType(position: Int): Int = when (getItem(position)) {
        is HistoryViewItem.HistoryItem -> HistoryViewHolder.LAYOUT_ID
        is HistoryViewItem.HistoryGroupItem -> HistoryGroupViewHolder.LAYOUT_ID
        is HistoryViewItem.TimeGroupHeader -> TimeGroupViewHolder.LAYOUT_ID
        is HistoryViewItem.RecentlyClosedItem -> RecentlyClosedViewHolder.LAYOUT_ID
        is HistoryViewItem.SyncedHistoryItem -> SyncedHistoryViewHolder.LAYOUT_ID
        is HistoryViewItem.EmptyHistoryItem -> EmptyViewHolder.LAYOUT_ID
        is HistoryViewItem.SignInHistoryItem -> SignInViewHolder.LAYOUT_ID
        is HistoryViewItem.TimeGroupSeparatorHistoryItem -> TimeGroupSeparatorViewHolder.LAYOUT_ID
        is HistoryViewItem.TopSeparatorHistoryItem -> TopSeparatorViewHolder.LAYOUT_ID
        else -> throw IllegalStateException("Unknown dataType.")
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(viewType, parent, false)
        return when (viewType) {
            HistoryViewHolder.LAYOUT_ID -> HistoryViewHolder(
                view,
                historyInteractor,
                this,
                ::onDeleteClicked
            )
            HistoryGroupViewHolder.LAYOUT_ID -> HistoryGroupViewHolder(
                view,
                historyInteractor,
                this,
                ::onDeleteClicked
            )
            TimeGroupViewHolder.LAYOUT_ID -> TimeGroupViewHolder(view) { _, _ ->
                // Will be implemented as the next step as part of breaking down this
                // PR: https://github.com/mozilla-mobile/fenix/pull/25879
            }
            RecentlyClosedViewHolder.LAYOUT_ID -> RecentlyClosedViewHolder(view, historyInteractor)
            SyncedHistoryViewHolder.LAYOUT_ID -> SyncedHistoryViewHolder(view, historyInteractor)
            EmptyViewHolder.LAYOUT_ID -> EmptyViewHolder(view)
            SignInViewHolder.LAYOUT_ID -> SignInViewHolder(view, historyInteractor)
            TimeGroupSeparatorViewHolder.LAYOUT_ID -> TimeGroupSeparatorViewHolder(view)
            TopSeparatorViewHolder.LAYOUT_ID -> TopSeparatorViewHolder(view)
            else -> throw IllegalStateException("Unknown viewType.")
        }
    }

    @Suppress("ComplexMethod")
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position) ?: return

        when (holder) {
            is HistoryViewHolder -> holder.bind(item as HistoryViewItem.HistoryItem, mode)
            is HistoryGroupViewHolder -> {
                // Items inside a group might be pending to be removed, so we have to adjust the
                // number of items on a group.
                val groupMetaData = (item as HistoryViewItem.HistoryGroupItem).data.items
                val groupPendingDeletionCount = groupMetaData.count { historyMetadata ->
                    pendingDeletionItems.find {
                        it is PendingDeletionHistory.MetaData &&
                            it.key == historyMetadata.historyMetadataKey &&
                            it.visitedAt == historyMetadata.visitedAt
                    } != null
                }
                holder.bind(item, mode, groupPendingDeletionCount)
            }
            is TimeGroupViewHolder -> holder.bind(item as HistoryViewItem.TimeGroupHeader)
            is RecentlyClosedViewHolder -> holder.bind(item as HistoryViewItem.RecentlyClosedItem)
            is SyncedHistoryViewHolder -> holder.bind(item as HistoryViewItem.SyncedHistoryItem)
            is EmptyViewHolder -> {
                holder.bind(item as HistoryViewItem.EmptyHistoryItem)
                // Resize emptyViewHolder to fill the rest of the recyclerview space.
                val lastItemView = holder.itemView
                lastItemView.viewTreeObserver.addOnGlobalLayoutListener {
                    recycler?.height?.let { recyclerViewHeight ->
                        val lastItemBottom = lastItemView.bottom
                        val heightDifference = recyclerViewHeight - lastItemBottom
                        if (heightDifference > 0) {
                            lastItemView.layoutParams.height = lastItemView.height + heightDifference
                            lastItemView.requestLayout()
                        }
                    }
                }
            }
        }
    }

    private fun onDeleteClicked(adapterPosition: Int) {
        // The click might have happened during animation.
        if (adapterPosition == RecyclerView.NO_POSITION) return

        getItem(adapterPosition)?.let {
            when (it) {
                is HistoryViewItem.HistoryItem -> it.data
                is HistoryViewItem.HistoryGroupItem -> it.data
                else -> null
            }?.let { historyItem ->
                val headerToRemove = calculateTimeGroupsToRemove(setOf(historyItem))
                historyInteractor.onDeleteHistoryItems(setOf(historyItem), headerToRemove)
            }
        }
    }

    /**
     * A helper method for [HistoryFragment] to decide if after removal of multiple items any of
     * headers should be removed as well.
     */
    @Suppress("NestedBlockDepth")
    fun calculateTimeGroupsToRemove(
        removedItems: Set<History>,
        snapshot: List<HistoryViewItem> = snapshot().items
    ): Set<HistoryItemTimeGroup> {
        val headersToRemove: MutableSet<HistoryItemTimeGroup> = mutableSetOf()

        // Group selected for removal items into timeGroup buckets, and rely on a bucket size to
        // determine if all items under a header have been removed.
        val timeGroupMap: MutableMap<HistoryItemTimeGroup, MutableSet<History>> =
            mutableMapOf()
        for (historyItem in removedItems) {
            if (!timeGroupMap.contains(historyItem.historyTimeGroup)) {
                timeGroupMap[historyItem.historyTimeGroup] = mutableSetOf(historyItem)
            }
            timeGroupMap[historyItem.historyTimeGroup]!!.add(historyItem)
        }

        // Calculate if the number of items between two different headers matches the bucket size of
        // items to be removed. If true, the header should be hidden.
        var previousTimeGroupPosition = 0
        var previousTimeGroup: HistoryItemTimeGroup? = null
        for ((index, item) in snapshot.withIndex()) {
            if (item is HistoryViewItem.TimeGroupHeader) {
                if (previousTimeGroup != null) {
                    // Additional subtraction comes from a [HistoryViewItem.TimeGroupSeparatorHistoryItem],
                    // that is always above a non collapsed [HistoryViewItem.TimeGroupHeader].
                    // Except the very first item, which is irrelevant in this case.
                    val timeGroupSize = (index - previousTimeGroupPosition) - 2
                    if (timeGroupMap[previousTimeGroup]!!.size == timeGroupSize) {
                        headersToRemove.add(previousTimeGroup)
                    }
                    previousTimeGroup = null
                }
                if (timeGroupMap.contains(item.timeGroup)) {
                    previousTimeGroupPosition = index
                    previousTimeGroup = item.timeGroup
                }
            }
        }

        // If there is only one timeGroup in the list, check if all its items were selected for
        // removal.
        if (previousTimeGroup != null) {
            val timeGroupSize = (snapshot.size - previousTimeGroupPosition) - 1
            if (timeGroupMap[previousTimeGroup]!!.size == timeGroupSize) {
                headersToRemove.add(previousTimeGroup)
            }
        }
        return headersToRemove
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        recycler = recyclerView
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        recycler = null
    }

    fun updateMode(mode: HistoryFragmentState.Mode) {
        this.mode = mode
        // Update the delete button alpha that the first item holds
        if (itemCount > 0) notifyItemChanged(0)
    }

    /**
     * @param pendingDeletionItems is used to filter out the items that should not be displayed.
     */
    fun updatePendingDeletionItems(pendingDeletionItems: Set<PendingDeletionHistory>) {
        this.pendingDeletionItems = pendingDeletionItems
    }

    companion object {
        private val historyDiffCallback = object : DiffUtil.ItemCallback<HistoryViewItem>() {
            override fun areItemsTheSame(
                oldItem: HistoryViewItem,
                newItem: HistoryViewItem
            ): Boolean {
                return oldItem == newItem
            }

            override fun areContentsTheSame(
                oldItem: HistoryViewItem,
                newItem: HistoryViewItem
            ): Boolean {
                return oldItem == newItem
            }

            override fun getChangePayload(
                oldItem: HistoryViewItem,
                newItem: HistoryViewItem
            ): Any {
                return newItem
            }
        }
    }
}
