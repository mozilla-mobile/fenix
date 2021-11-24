/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.library.history

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.DiffUtil
import org.mozilla.fenix.R
import org.mozilla.fenix.components.history.PagedHistoryProvider
import org.mozilla.fenix.selection.SelectionHolder
import org.mozilla.fenix.library.history.viewholders.HistoryListItemViewHolder
import org.mozilla.fenix.utils.Settings

enum class HistoryItemTimeGroup {
    Today, Yesterday, ThisWeek, ThisMonth, Older;

    fun humanReadable(context: Context): String = when (this) {
        Today -> context.getString(R.string.history_today)
        Yesterday -> context.getString(R.string.history_yesterday)
        ThisWeek -> context.getString(R.string.history_7_days)
        ThisMonth -> context.getString(R.string.history_30_days)
        Older -> context.getString(R.string.history_older)
    }
}

class HistoryAdapter(
    private val historyInteractor: HistoryInteractor,
    private val pagedHistoryProvider: PagedHistoryProvider,
    private val settings: Settings
) : PagedListAdapter<History, HistoryListItemViewHolder>(historyDiffCallback),
    SelectionHolder<History> {

    private var mode: HistoryFragmentState.Mode = HistoryFragmentState.Mode.Normal
    override val selectedItems get() = mode.selectedItems
    var pendingDeletionIds = emptySet<Long>()

    override fun getItemViewType(position: Int): Int = HistoryListItemViewHolder.LAYOUT_ID

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryListItemViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(viewType, parent, false)

        return HistoryListItemViewHolder(view, historyInteractor, this, settings)
    }

    fun updateMode(mode: HistoryFragmentState.Mode) {
        this.mode = mode
        // Update the delete button alpha that the first item holds
        if (itemCount > 0) notifyItemChanged(0)
    }

    override fun onBindViewHolder(holder: HistoryListItemViewHolder, position: Int) {
        val current = getItem(position) ?: return
        val historyItemTimeGroup = HistoryViewModel.timeGroupForHistoryItem(current)
        val isPendingDeletion = pendingDeletionIds.contains(current.visitedAt)

        val timeGroup: HistoryItemTimeGroup? = when {
            position == 0 -> historyItemTimeGroup
            isPendingDeletion -> null
            else -> {
                val previous = getItem(position - 1) ?: return
                val headerForPreviousItem = HistoryViewModel.timeGroupForHistoryItem(previous)
                if (historyItemTimeGroup != headerForPreviousItem) {
                    historyItemTimeGroup
                } else {
                    null
                }
            }
        }

        holder.bind(current, pagedHistoryProvider, timeGroup, position == 0, mode, isPendingDeletion)
    }

    fun updatePendingDeletionIds(pendingDeletionIds: Set<Long>) {
        this.pendingDeletionIds = pendingDeletionIds
    }

    companion object {
        private val historyDiffCallback = object : DiffUtil.ItemCallback<History>() {
            override fun areItemsTheSame(oldItem: History, newItem: History): Boolean {
                return oldItem == newItem
            }

            override fun areContentsTheSame(oldItem: History, newItem: History): Boolean {
                return oldItem == newItem
            }

            override fun getChangePayload(oldItem: History, newItem: History): Any? {
                return newItem
            }
        }
    }
}
