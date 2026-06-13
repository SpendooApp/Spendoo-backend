package org.spendoo.statistics.util

import org.spendoo.statistics.model.IntervalRange
import org.spendoo.statistics.model.StatsPeriod
import java.time.DayOfWeek
import java.time.LocalDateTime

import org.spendoo.i18n.I18nService
import org.spendoo.statistics.model.Language

object IntervalCalculator {

    fun getFirstChartIntervals(period: StatsPeriod, now: LocalDateTime, lang: Language, i18nService: I18nService): List<IntervalRange> {
        return when (period) {
            StatsPeriod.DAILY -> {
                val dayOfWeek = now.dayOfWeek
                val daysToSubtract = getDaysToSubtractForSaturdayStart(dayOfWeek)
                val sat = now.minusDays(daysToSubtract.toLong()).withHour(0).withMinute(0).withSecond(0).withNano(0)
                val labels = listOf("saturday", "sunday", "monday", "tuesday", "wednesday", "thursday", "friday").map {
                    i18nService.getMessage("day.$it", lang.locale)
                }
                labels.mapIndexed { idx, label ->
                    val start = sat.plusDays(idx.toLong())
                    val end = start.plusDays(1)
                    IntervalRange(label, start, end)
                }
            }
            StatsPeriod.WEEKLY -> {
                val firstOfMonth = now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0)
                val lengthOfMonth = now.toLocalDate().lengthOfMonth()
                val intervals = mutableListOf<IntervalRange>()
                for (w in 0..4) {
                    val startDay = w * 7 + 1
                    if (startDay > lengthOfMonth) break
                    val start = firstOfMonth.plusDays((startDay - 1).toLong())
                    val endDay = (w + 1) * 7
                    val end = if (endDay >= lengthOfMonth || w == 4) {
                        firstOfMonth.plusDays(lengthOfMonth.toLong())
                    } else {
                        firstOfMonth.plusDays(endDay.toLong())
                    }
                    val weekNum = w + 1
                    val label = i18nService.getMessage("week.number", lang.locale, null, weekNum)
                    intervals.add(IntervalRange(label, start, end))
                }
                intervals
            }
            StatsPeriod.MONTHLY -> {
                val startOfYear = now.withDayOfYear(1).withHour(0).withMinute(0).withSecond(0).withNano(0)
                val labels = listOf("january", "february", "march", "april", "may", "june", "july", "august", "september", "october", "november", "december").map {
                    i18nService.getMessage("month.$it", lang.locale)
                }
                labels.mapIndexed { idx, label ->
                    val start = startOfYear.plusMonths(idx.toLong())
                    val end = start.plusMonths(1)
                    IntervalRange(label, start, end)
                }
            }
            StatsPeriod.YEARLY -> {
                val currentYear = now.year
                val startYear = currentYear - 5
                val intervals = mutableListOf<IntervalRange>()
                for (y in startYear..currentYear) {
                    val start = LocalDateTime.of(y, 1, 1, 0, 0)
                    val end = LocalDateTime.of(y + 1, 1, 1, 0, 0)
                    intervals.add(IntervalRange(y.toString(), start, end))
                }
                intervals
            }
        }
    }

    fun getSecondChartIntervals(period: StatsPeriod, now: LocalDateTime, lang: Language, i18nService: I18nService): List<IntervalRange> {
        return when (period) {
            StatsPeriod.DAILY -> {
                val todayStart = now.withHour(0).withMinute(0).withSecond(0).withNano(0)
                (0..5).map { i ->
                    val start = todayStart.minusDays((5 - i).toLong())
                    val end = start.plusDays(1)
                    val label = i18nService.getMessage("day.${start.dayOfWeek.name.lowercase()}", lang.locale)
                    IntervalRange(label, start, end)
                }
            }
            StatsPeriod.WEEKLY -> {
                val dayOfWeek = now.dayOfWeek
                val daysToSubtract = getDaysToSubtractForSaturdayStart(dayOfWeek)
                val currentWeekSat = now.minusDays(daysToSubtract.toLong()).withHour(0).withMinute(0).withSecond(0).withNano(0)
                (0..5).map { i ->
                    val start = currentWeekSat.minusWeeks((5 - i).toLong())
                    val end = start.plusWeeks(1)
                    val weekNum = i + 1
                    val label = i18nService.getMessage("week.number", lang.locale, null, weekNum)
                    IntervalRange(label, start, end)
                }
            }
            StatsPeriod.MONTHLY -> {
                val firstOfCurrentMonth = now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0)
                (0..5).map { i ->
                    val start = firstOfCurrentMonth.minusMonths((5 - i).toLong())
                    val end = start.plusMonths(1)
                    val label = i18nService.getMessage("month.${start.month.name.lowercase()}", lang.locale)
                    IntervalRange(label, start, end)
                }
            }
            StatsPeriod.YEARLY -> {
                val currentYear = now.year
                val startYear = currentYear - 5
                (startYear..currentYear).map { y ->
                    val start = LocalDateTime.of(y, 1, 1, 0, 0)
                    val end = LocalDateTime.of(y + 1, 1, 1, 0, 0)
                    IntervalRange(y.toString(), start, end)
                }
            }
        }
    }

    fun getPreviousPeriodRange(period: StatsPeriod, periodStart: LocalDateTime, periodEnd: LocalDateTime): Pair<LocalDateTime, LocalDateTime> {
        return when (period) {
            StatsPeriod.DAILY -> Pair(periodStart.minusWeeks(1), periodEnd.minusWeeks(1))
            StatsPeriod.WEEKLY -> Pair(periodStart.minusMonths(1), periodEnd.minusMonths(1))
            StatsPeriod.MONTHLY -> Pair(periodStart.minusYears(1), periodEnd.minusYears(1))
            StatsPeriod.YEARLY -> Pair(periodStart.minusYears(6), periodEnd.minusYears(6))
        }
    }

    private fun getDaysToSubtractForSaturdayStart(dayOfWeek: DayOfWeek): Int {
        return when (dayOfWeek) {
            DayOfWeek.SATURDAY -> 0
            DayOfWeek.SUNDAY -> 1
            DayOfWeek.MONDAY -> 2
            DayOfWeek.TUESDAY -> 3
            DayOfWeek.WEDNESDAY -> 4
            DayOfWeek.THURSDAY -> 5
            DayOfWeek.FRIDAY -> 6
        }
    }
}
