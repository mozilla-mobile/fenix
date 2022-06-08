package org.mozilla.fenix.library.history

import java.util.*

enum class RemoveTimeGroup {
    OneHour,
    Today,
    LastWeek;

    fun timeFrameForTimeGroup() : Pair<Long, Long> {
        return when (this) {
            OneHour -> {
                // for testing
//                val oneMinuteRange = LongRange(getMinutesAgo(1).time, getHourAgo(0).time)
//                Pair(oneMinuteRange.first, oneMinuteRange.last)

                // for real
                val oneHourRange = LongRange(getHourAgo(1).time, getHourAgo(0).time)
                Pair(oneHourRange.first, oneHourRange.last)
            }
            Today -> {
                val lastWeekRange = LongRange(getDaysAgo(zeroDays).time, Long.MAX_VALUE)
                Pair(lastWeekRange.first, lastWeekRange.last)
            }
            LastWeek -> {
                val lastWeekRange = LongRange(getDaysAgo(sevenDays).time, getDaysAgo(oneDay).time)
                Pair(lastWeekRange.first, lastWeekRange.last)
            }
        }
    }

    companion object {
        private const val fiveMinutes = 5
        private const val oneHour = 1
        private const val zeroDays = 0
        private const val oneDay = 1
        private const val sevenDays = 7
        private const val thirtyDays = 30
        private val oneHourAgo = getHourAgo(oneHour).time
        private val fiveMinutesAgo = getMinutesAgo(fiveMinutes).time
        private val now = getHourAgo(0).time
        private val today = getDaysAgo(zeroDays).time
        private val yesterday = getDaysAgo(oneDay).time
        private val sevenDaysAgo = getDaysAgo(sevenDays).time
        private val thirtyDaysAgo = getDaysAgo(thirtyDays).time
        internal val oneHourRange = LongRange(oneHourAgo, now)
        internal val fiveMinutesRange = LongRange(fiveMinutesAgo, now)
        private val todayRange = LongRange(today, Long.MAX_VALUE) // all future time is considered today
        private val yesterdayRange = LongRange(yesterday, today)
        private val lastWeekRange = LongRange(sevenDaysAgo, yesterday)
        private val lastMonthRange = LongRange(thirtyDaysAgo, sevenDaysAgo)

        private fun getLongRange() : LongRange {
            return LongRange(getMinutesAgo(fiveMinutes).time, getHourAgo(0).time)
        }

        private fun getMinutesAgo(minutes: Int): Date {
            return Calendar.getInstance().apply {
                set(Calendar.MINUTE, this.get(Calendar.MINUTE) - minutes)
            }.time
        }

        private fun getHourAgo(hoursAgo: Int): Date {
            return Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, this.get(Calendar.HOUR_OF_DAY) - hoursAgo)
            }.time
        }

        private fun getDaysAgo(daysAgo: Int): Date {
            return Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                add(Calendar.DAY_OF_YEAR, -daysAgo)
            }.time
        }



        internal fun timeGroupForTimestamp(timestamp: Long): HistoryItemTimeGroup {
            return when {
                todayRange.contains(timestamp) -> HistoryItemTimeGroup.Today
                yesterdayRange.contains(timestamp) -> HistoryItemTimeGroup.Yesterday
                lastWeekRange.contains(timestamp) -> HistoryItemTimeGroup.ThisWeek
                lastMonthRange.contains(timestamp) -> HistoryItemTimeGroup.ThisMonth
                else -> HistoryItemTimeGroup.Older
            }
        }
    }
}
