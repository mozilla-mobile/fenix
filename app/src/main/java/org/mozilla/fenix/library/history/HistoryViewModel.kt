/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.library.history

import android.text.format.DateUtils
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import org.mozilla.fenix.components.history.PagedHistoryProvider
import java.util.Calendar
import java.util.Date

class HistoryViewModel(historyProvider: PagedHistoryProvider) : ViewModel() {
    var history: LiveData<PagedList<History>>
    var userHasHistory = MutableLiveData(true)
    private val datasource: LiveData<HistoryDataSource>

    init {
        val historyDataSourceFactory = HistoryDataSourceFactory(historyProvider)
        datasource = historyDataSourceFactory.datasource

        history = LivePagedListBuilder(historyDataSourceFactory, PAGE_SIZE)
            .setBoundaryCallback(object : PagedList.BoundaryCallback<History>() {
                override fun onZeroItemsLoaded() {
                    userHasHistory.value = false
                }
            })
            .build()
    }

    fun invalidate() {
        datasource.value?.invalidate()
    }

    companion object {
        private const val PAGE_SIZE = 25
        private const val zeroDays = 0
        private const val oneDay = 1
        private const val sevenDays = 7
        private const val thirtyDays = 30
        private val zeroDaysAgo = getDaysAgo(zeroDays).time
        private val oneDayAgo = getDaysAgo(oneDay).time
        private val sevenDaysAgo = getDaysAgo(sevenDays).time
        private val thirtyDaysAgo = getDaysAgo(thirtyDays).time
        private val yesterdayRange = LongRange(oneDayAgo, zeroDaysAgo)
        private val lastWeekRange = LongRange(sevenDaysAgo, oneDayAgo)
        private val lastMonthRange = LongRange(thirtyDaysAgo, sevenDaysAgo)

        private fun getDaysAgo(daysAgo: Int): Date {
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.DAY_OF_YEAR, -daysAgo)

            return calendar.time
        }

        fun timeGroupForHistoryItem(item: History): HistoryItemTimeGroup {
            return timeGroupForTimestamp(item.visitedAt)
        }

        fun timeGroupForTimestamp(timestamp: Long): HistoryItemTimeGroup {
            return when {
                DateUtils.isToday(timestamp) -> HistoryItemTimeGroup.Today
                yesterdayRange.contains(timestamp) -> HistoryItemTimeGroup.Yesterday
                lastWeekRange.contains(timestamp) -> HistoryItemTimeGroup.ThisWeek
                lastMonthRange.contains(timestamp) -> HistoryItemTimeGroup.ThisMonth
                else -> HistoryItemTimeGroup.Older
            }
        }
    }
}
