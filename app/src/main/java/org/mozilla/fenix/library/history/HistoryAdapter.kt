/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.library.history

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import org.mozilla.fenix.R
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
    SelectionHolder<History>, HeaderManager {

    private var recycler: RecyclerView? = null

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        recycler = recyclerView
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        recycler = null
    }

    private var mode: HistoryFragmentState.Mode = HistoryFragmentState.Mode.Normal
    private var pendingDeletionItems = emptySet<PendingDeletionHistory>()
    private val headerPositions: MutableMap<HistoryItemTimeGroup, Int> = mutableMapOf()

    // A flag to track the empty state of the list. Items are not being deleted immediately,
    // but hidden from the UI until the Undo snackbar will execute the delayed operation.
    // Whether the adapter has actually zero items or all present items are hidden,
    // the screen should be updated into proper empty/not empty state.
    private var isEmpty = true

    init {
        // When data flow changes, there is a chance that empty state has changed as well
        addOnPagesUpdatedListener {
            isEmpty = true
        }
    }

    override val selectedItems
        get() = mode.selectedItems

    override fun getItemViewType(position: Int): Int = when (getItem(position)!!) {
        is HistoryViewItem.HistoryItem -> HistoryViewHolder.LAYOUT_ID
        is HistoryViewItem.HistoryGroupItem -> HistoryGroupViewHolder.LAYOUT_ID
        is HistoryViewItem.TimeGroupHeader -> TimeGroupViewHolder.LAYOUT_ID
        is HistoryViewItem.RecentlyClosedItem -> RecentlyClosedViewHolder.LAYOUT_ID
        is HistoryViewItem.SyncedHistoryItem -> SyncedHistoryViewHolder.LAYOUT_ID
        is HistoryViewItem.EmptyHistoryItem -> EmptyViewHolder.LAYOUT_ID
        is HistoryViewItem.SignInHistoryItem -> SignInViewHolder.LAYOUT_ID
        is HistoryViewItem.TimeGroupSeparatorHistoryItem -> TimeGroupSeparatorViewHolder.LAYOUT_ID
        is HistoryViewItem.TopSeparatorHistoryItem -> TopSeparatorViewHolder.LAYOUT_ID
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
            TimeGroupViewHolder.LAYOUT_ID -> TimeGroupViewHolder(view, historyInteractor)
            RecentlyClosedViewHolder.LAYOUT_ID -> RecentlyClosedViewHolder(view, historyInteractor)
            SyncedHistoryViewHolder.LAYOUT_ID -> SyncedHistoryViewHolder(view, historyInteractor)
            EmptyViewHolder.LAYOUT_ID -> EmptyViewHolder(view)
            SignInViewHolder.LAYOUT_ID -> SignInViewHolder(view)
            TimeGroupSeparatorViewHolder.LAYOUT_ID -> TimeGroupSeparatorViewHolder(view)
            TopSeparatorViewHolder.LAYOUT_ID -> TopSeparatorViewHolder(view)
            else -> throw IllegalStateException("Unknown viewType.")
        }
    }

    @Suppress("ComplexMethod")
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position) ?: return
        Log.d("UndoDebugging", "binding, position = $position")

        // If there is a single visible item, it's enough to change the empty state of the view.
        if (isEmpty && item is HistoryViewItem.HistoryItem ||
            item is HistoryViewItem.HistoryGroupItem ||
            item is HistoryViewItem.TimeGroupHeader
        ) {
            isEmpty = false
            onEmptyStateChanged.invoke(isEmpty)
        } else if (position + 1 == itemCount) {
            // If we reached the bottom of the list and there still has been zero visible items,
            // we can can change the History view state to empty.
            if (isEmpty) {
                onEmptyStateChanged.invoke(isEmpty)
            }
        }

        when (holder) {
            is HistoryViewHolder -> holder.bind(item as HistoryViewItem.HistoryItem, mode)
            is HistoryGroupViewHolder -> {
                val groupPendingDeletionCount =
                    (item as HistoryViewItem.HistoryGroupItem).data.items.count { historyMetadata ->
                        pendingDeletionItems.find {
                            it is PendingDeletionHistory.MetaData &&
                                    it.key == historyMetadata.historyMetadataKey &&
                                    it.visitedAt == historyMetadata.visitedAt
                        } != null
                    }
                holder.bind(item, mode, groupPendingDeletionCount)
            }
            is TimeGroupViewHolder -> {
                val timeGroup = (item as HistoryViewItem.TimeGroupHeader).timeGroup
                headerPositions[timeGroup] = position
                holder.bind(item)
            }
            is RecentlyClosedViewHolder -> holder.bind(item as HistoryViewItem.RecentlyClosedItem)
            is SyncedHistoryViewHolder -> holder.bind(item as HistoryViewItem.SyncedHistoryItem)
            is EmptyViewHolder -> holder.bind(item as HistoryViewItem.EmptyHistoryItem)
            is SignInViewHolder -> holder.bind(item as HistoryViewItem.SignInHistoryItem)
        }

        // Change emptyViewHolder size to fit the rest of the recyclerview space
        if (holder is EmptyViewHolder) {
            val lastItemView = holder.itemView
            lastItemView.viewTreeObserver.addOnGlobalLayoutListener {
                val recyclerViewHeight = recycler?.height ?: 0 // TODO check
                val lastItemBottom = lastItemView.bottom
                val heightDifference = recyclerViewHeight - lastItemBottom
                if (heightDifference > 0) {
                    lastItemView.layoutParams.height = lastItemView.height + heightDifference
                    lastItemView.requestLayout()
                }
            }
        }
    }

    private fun onDeleteClicked(adapterPosition: Int) {
        val item = getItem(adapterPosition)
        item?.let {
            if (it is HistoryViewItem.HistoryItem) {
                val previousItem = getItem(adapterPosition - 1)
                val nextItem = if (adapterPosition < itemCount - 1) {
                    getItem(adapterPosition + 1)
                } else {
                    null
                }
                if (previousItem is HistoryViewItem.TimeGroupHeader
                    && (nextItem is HistoryViewItem.TimeGroupHeader
                            || nextItem == null) // TODO change to Empty
                ) {
                    historyInteractor.onDeleteSome(setOf(it.data), setOf(it.data.historyTimeGroup))
                } else {
                    historyInteractor.onDeleteSome(setOf(it.data), setOf())
                }
            } else if (it is HistoryViewItem.HistoryGroupItem) {
                val previousItem = getItem(adapterPosition - 1)
                val nextItem = if (adapterPosition == itemCount) {
                    getItem(adapterPosition + 1)
                } else {
                    null
                }
                if (previousItem is HistoryViewItem.TimeGroupHeader
                    && (nextItem is HistoryViewItem.TimeGroupHeader
                            || nextItem == null) // TODO change to Empty
                ) {
                    historyInteractor.onDeleteSome(setOf(it.data), setOf(it.data.historyTimeGroup))
                } else {
                    historyInteractor.onDeleteSome(setOf(it.data), setOf())
                }
            }
        }
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
        Log.d(
            "UndoDebugging",
            "updatePendingDeletionItems, pendingDeletionItems = $pendingDeletionItems"
        )
        this.pendingDeletionItems = pendingDeletionItems
    }

    companion object {
        private val historyDiffCallback = object : DiffUtil.ItemCallback<HistoryViewItem>() {
            override fun areItemsTheSame(
                oldItem: HistoryViewItem,
                newItem: HistoryViewItem
            ): Boolean {
                return if (oldItem is HistoryViewItem.TimeGroupHeader && newItem is HistoryViewItem.TimeGroupHeader) {
                    oldItem.timeGroup == newItem.timeGroup
                } else {
                    oldItem == newItem
                }
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

    override fun getHeaderPositionForItem(itemPosition: Int): Int {
        Log.d("CollapseDebugging", "getHeaderPositionForItem, itemPosition = $itemPosition")
        val item: HistoryViewItem? = getItem(itemPosition)

//        if (item == null) {
//            Log.d("stickyHeader", "getHeaderPositionForItem, item = null")
//            return -1
//        }

        val result = when (item) {
            is HistoryViewItem.TimeGroupHeader -> headerPositions[item.timeGroup]
            is HistoryViewItem.HistoryGroupItem -> headerPositions[item.data.historyTimeGroup]
            is HistoryViewItem.HistoryItem -> headerPositions[item.data.historyTimeGroup]
            else -> -1
        }
        Log.d("stickyHeader", "getHeaderPositionForItem, result = $result")
        return result ?: -1

//
//        val timeGroup = when (item) {
//            is HistoryViewItem.TimeGroupHeader -> item.timeGroup
//            is HistoryViewItem.HistoryGroupItem -> item.data.historyTimeGroup
//            is HistoryViewItem.HistoryItem -> item.data.historyTimeGroup
//            is HistoryViewItem.RecentlyClosedItem -> -1
//            is HistoryViewItem.SyncedHistoryItem -> -1
//        }
//        val result = headerPositions[timeGroup] ?: -1
//        Log.d("stickyHeader", "getHeaderPositionForItem, result = $result")
//        return result
//        return getItem(itemPosition)?.let {
//
//        } ?: 0
    }

    override fun getHeaderLayout(headerPosition: Int): Int {
        Log.d("stickyHeader", "getHeaderLayout, headerPosition = $headerPosition")
        return R.layout.history_list_header
    }

    override fun bindHeader(header: View, headerPosition: Int) {
        Log.d(
            "stickyHeader",
            "bindHeaderData, header = ${header}, headerPosition = $headerPosition"
        )
        val headerData = getItem(headerPosition)
        if (headerData is HistoryViewItem.TimeGroupHeader) {
            val textView = header.findViewById<TextView>(R.id.header_title)
            textView.text = headerData.title

            val imageView = header.findViewById<ImageView>(R.id.chevron)
            imageView.isActivated = headerData.collapsed
        }
    }

    override fun isHeader(itemPosition: Int): Boolean {
        if (itemPosition == -1) return false // TODO check
        Log.d("stickyHeader", "isHeader, itemPosition = $itemPosition")
        val item = getItem(itemPosition) ?: return false
        return item is HistoryViewItem.TimeGroupHeader
    }

    // TODO change to item position
    override fun onStickyHeaderClicked(itemPosition: Int) {
//        if (itemPosition == -1) return // TODO check
        val item = getItem(itemPosition) ?: return
        val timeGroup = when (item) {
            is HistoryViewItem.HistoryItem -> item.data.historyTimeGroup
            is HistoryViewItem.HistoryGroupItem -> item.data.historyTimeGroup
            is HistoryViewItem.TimeGroupHeader -> item.timeGroup
            else -> return
        }
        val headerPosition = headerPositions[timeGroup]!!
        val headerData = getItem(headerPosition) as HistoryViewItem.TimeGroupHeader
        historyInteractor.onTimeGroupClicked(
            headerData.timeGroup,
            headerData.collapsed
        )
    }
}
