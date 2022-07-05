/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.library.history

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
    SelectionHolder<History>,
    HeaderManager {

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

    init {
//         Tracking updates of data flow. Delete data flow might have filtered out the last history
//         or group history item, so the empty view should be displayed. Listener is triggered on
//         every change, including loading additional items, so we don't want to do extra checks
//         after there are more items in the adapter than in a single load.
        addOnPagesUpdatedListener {
            if (itemCount <= HistoryViewItemDataSource.PAGE_SIZE) {
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

    override val selectedItems
        get() = mode.selectedItems

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
            is TimeGroupViewHolder -> {
                // We want to track positions of Headers in order to draw sticky header correctly.
                val timeGroup = (item as HistoryViewItem.TimeGroupHeader).timeGroup
                headerPositions[timeGroup] = position
                holder.bind(item)
            }
            is RecentlyClosedViewHolder -> holder.bind(item as HistoryViewItem.RecentlyClosedItem)
            is SyncedHistoryViewHolder -> holder.bind(item as HistoryViewItem.SyncedHistoryItem)
            is EmptyViewHolder -> holder.bind(item as HistoryViewItem.EmptyHistoryItem)
            is SignInViewHolder -> holder.bind(item as HistoryViewItem.SignInHistoryItem)
        }

        // Change emptyViewHolder size to fill the rest of the recyclerview space.
        if (holder is EmptyViewHolder) {
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

    private fun onDeleteClicked(adapterPosition: Int) {
        getItem(adapterPosition)?.let {
            when (it) {
                is HistoryViewItem.HistoryItem -> it.data
                is HistoryViewItem.HistoryGroupItem -> it.data
                else -> null
            }?.let { data ->
                val previousItem = getItem(adapterPosition - 1)
                val nextItem = if (adapterPosition < itemCount - 1) {
                    getItem(adapterPosition + 1)
                } else {
                    null
                }
                // If the item above is a header and there are no items below or there is another
                // header, we remove the header as well
                if (previousItem is HistoryViewItem.TimeGroupHeader &&
                    (nextItem is HistoryViewItem.TimeGroupSeparatorHistoryItem || nextItem == null)
                ) {
                    historyInteractor.onDeleteSome(setOf(data), setOf(data.historyTimeGroup))
                } else {
                    historyInteractor.onDeleteSome(setOf(data))
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
     * @param pendingDeletionItems is used to adjust the number of items inside a group.
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
                // TimeGroup will change collapsed parameter, but not the position.
                return if (oldItem is HistoryViewItem.TimeGroupHeader &&
                    newItem is HistoryViewItem.TimeGroupHeader
                ) {
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
        val position = getItem(itemPosition)?.let { item ->
            val timeGroup = when (item) {
                is HistoryViewItem.TimeGroupHeader -> item.timeGroup
                is HistoryViewItem.HistoryGroupItem -> item.data.historyTimeGroup
                is HistoryViewItem.HistoryItem -> item.data.historyTimeGroup
                is HistoryViewItem.TimeGroupSeparatorHistoryItem -> item.timeGroup
                else -> null
            }
            timeGroup?.let {
                headerPositions[timeGroup]
            }
        } ?: -1
        return position
    }

    override fun getHeaderLayout(headerPosition: Int) = R.layout.history_list_header

    override fun bindHeader(header: View, headerPosition: Int) {
        // Populate sticky header with the correct data.
        getItem(headerPosition)?.let {
            if (it is HistoryViewItem.TimeGroupHeader) {
                header.findViewById<TextView>(R.id.header_title).apply {
                    text = it.title
                }
                header.findViewById<ImageView>(R.id.chevron).apply {
                    isActivated = it.collapsed
                }
            }
        }
    }

    override fun isHeader(itemPosition: Int): Boolean {
        return getItem(itemPosition)?.let {
            it is HistoryViewItem.TimeGroupHeader
        } ?: false
    }
}
