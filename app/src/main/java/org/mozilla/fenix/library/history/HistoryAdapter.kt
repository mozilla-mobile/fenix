/* This Source Code Form is subject to the terms of the Mozilla Public
   License, v. 2.0. If a copy of the MPL was not distributed with this
   file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.library.history

import android.content.Context
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import io.reactivex.Observer
import kotlinx.coroutines.Job
import org.mozilla.fenix.R
import org.mozilla.fenix.library.history.viewholders.HistoryDeleteButtonViewHolder
import org.mozilla.fenix.library.history.viewholders.HistoryHeaderViewHolder
import org.mozilla.fenix.library.history.viewholders.HistoryListItemViewHolder
import java.lang.IllegalStateException
import java.util.Date
import java.util.Calendar

private sealed class AdapterItem {
    object DeleteButton : AdapterItem()
    data class SectionHeader(val range: Range) : AdapterItem()
    data class Item(val item: HistoryItem) : AdapterItem()
}

private enum class Range {
    Today, ThisWeek, ThisMonth, Older;

    fun humanReadable(context: Context): String = when (this) {
        Today -> context.getString(R.string.history_today)
        ThisWeek -> context.getString(R.string.history_this_week)
        ThisMonth -> context.getString(R.string.history_this_month)
        Older -> context.getString(R.string.history_older)
    }
}

private class HistoryList(val history: List<HistoryItem>) {
    val items: List<AdapterItem>

    init {
        val oneDayAgo = getDaysAgo(zero_days).time
        val sevenDaysAgo = getDaysAgo(seven_days).time
        val thirtyDaysAgo = getDaysAgo(thirty_days).time

        val lastWeek = LongRange(sevenDaysAgo, oneDayAgo)
        val lastMonth = LongRange(thirtyDaysAgo, sevenDaysAgo)
        val items = mutableListOf<AdapterItem>()
        items.add(AdapterItem.DeleteButton)

        val groups = history.groupBy { item ->
            when {
                DateUtils.isToday(item.visitedAt) -> Range.Today
                lastWeek.contains(item.visitedAt) -> Range.ThisWeek
                lastMonth.contains(item.visitedAt) -> Range.ThisMonth
                else -> Range.Older
            }
        }

        items.addAll(groups.adapterItemsForRange(Range.Today))
        items.addAll(groups.adapterItemsForRange(Range.ThisWeek))
        items.addAll(groups.adapterItemsForRange(Range.ThisMonth))
        items.addAll(groups.adapterItemsForRange(Range.Older))
        this.items = items
    }

    private fun Map<Range, List<HistoryItem>>.adapterItemsForRange(range: Range): List<AdapterItem> {
        return this[range]?.let { historyItems ->
            val items = mutableListOf<AdapterItem>()
            if (historyItems.isNotEmpty()) {
                items.add(AdapterItem.SectionHeader(range))
                for (item in historyItems) {
                    items.add(AdapterItem.Item(item))
                }
            }
            items
        } ?: listOf()
    }

    companion object {
        private const val zero_days = 0
        private const val seven_days = 7
        private const val thirty_days = 30

        private fun getDaysAgo(daysAgo: Int): Date {
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.DAY_OF_YEAR, -daysAgo)

            return calendar.time
        }
    }
}

class HistoryAdapter(
    private val actionEmitter: Observer<HistoryAction>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private var historyList: HistoryList = HistoryList(emptyList())
    private var mode: HistoryState.Mode = HistoryState.Mode.Normal
    private lateinit var job: Job

    fun updateData(items: List<HistoryItem>, mode: HistoryState.Mode) {
        this.historyList = HistoryList(items)
        this.mode = mode
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = historyList.items.size

    override fun getItemViewType(position: Int): Int {
        return when (historyList.items[position]) {
            is AdapterItem.DeleteButton -> HistoryDeleteButtonViewHolder.LAYOUT_ID
            is AdapterItem.SectionHeader -> HistoryHeaderViewHolder.LAYOUT_ID
            is AdapterItem.Item -> HistoryListItemViewHolder.LAYOUT_ID
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(viewType, parent, false)

        return when (viewType) {
            HistoryDeleteButtonViewHolder.LAYOUT_ID -> HistoryDeleteButtonViewHolder(view, actionEmitter)
            HistoryHeaderViewHolder.LAYOUT_ID -> HistoryHeaderViewHolder(view)
            HistoryListItemViewHolder.LAYOUT_ID -> HistoryListItemViewHolder(view, actionEmitter, job)
            else -> throw IllegalStateException()
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is HistoryDeleteButtonViewHolder -> holder.bind(mode)
            is HistoryHeaderViewHolder -> historyList.items[position].also {
                if (it is AdapterItem.SectionHeader) {
                    holder.bind(it.range.humanReadable(holder.itemView.context))
                }
            }
            is HistoryListItemViewHolder -> (historyList.items[position] as AdapterItem.Item).also {
                holder.bind(it.item, mode)
            }
        }
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        job = Job()
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        job.cancel()
    }
}
