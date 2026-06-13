package org.spendoo.statistics.util

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test
import io.mockk.every
import io.mockk.mockk
import org.spendoo.i18n.I18nService
import org.spendoo.statistics.model.Language
import org.spendoo.statistics.model.StatsPeriod
import java.time.LocalDateTime

class IntervalCalculatorTest {

    @Test
    fun `getFirstChartIntervals DAILY returns 7 days from Saturday to Friday`() {
        val i18nService = mockk<I18nService>()
        every { i18nService.getMessage(any(), any(), any(), *anyVararg()) } returns "Day"
        
        val referenceDate = LocalDateTime.of(2026, 6, 11, 12, 0)
        val intervals = IntervalCalculator.getFirstChartIntervals(StatsPeriod.DAILY, referenceDate, Language.EN, i18nService)

        assertThat(intervals).hasSize(7)
        assertThat(intervals[0].start).isEqualTo(LocalDateTime.of(2026, 6, 6, 0, 0))
        assertThat(intervals[0].end).isEqualTo(LocalDateTime.of(2026, 6, 7, 0, 0))
        assertThat(intervals[6].start).isEqualTo(LocalDateTime.of(2026, 6, 12, 0, 0))
        assertThat(intervals[6].end).isEqualTo(LocalDateTime.of(2026, 6, 13, 0, 0))
    }

    @Test
    fun `getFirstChartIntervals WEEKLY returns weeks within current month`() {
        val i18nService = mockk<I18nService>()
        every { i18nService.getMessage(any(), any(), any(), *anyVararg()) } returns "W1"
        
        val referenceDate = LocalDateTime.of(2026, 6, 15, 12, 0)
        val intervals = IntervalCalculator.getFirstChartIntervals(StatsPeriod.WEEKLY, referenceDate, Language.EN, i18nService)

        assertThat(intervals).hasSize(5)
        assertThat(intervals[0].start).isEqualTo(LocalDateTime.of(2026, 6, 1, 0, 0))
        assertThat(intervals[0].end).isEqualTo(LocalDateTime.of(2026, 6, 8, 0, 0))
        assertThat(intervals[4].start).isEqualTo(LocalDateTime.of(2026, 6, 29, 0, 0))
        assertThat(intervals[4].end).isEqualTo(LocalDateTime.of(2026, 7, 1, 0, 0))
    }

    @Test
    fun `getFirstChartIntervals MONTHLY returns 12 months of current year`() {
        val i18nService = mockk<I18nService>()
        every { i18nService.getMessage(any(), any(), any(), *anyVararg()) } returns "Month"
        
        val referenceDate = LocalDateTime.of(2026, 6, 15, 12, 0)
        val intervals = IntervalCalculator.getFirstChartIntervals(StatsPeriod.MONTHLY, referenceDate, Language.EN, i18nService)

        assertThat(intervals).hasSize(12)
        assertThat(intervals[0].start).isEqualTo(LocalDateTime.of(2026, 1, 1, 0, 0))
        assertThat(intervals[0].end).isEqualTo(LocalDateTime.of(2026, 2, 1, 0, 0))
        assertThat(intervals[11].start).isEqualTo(LocalDateTime.of(2026, 12, 1, 0, 0))
        assertThat(intervals[11].end).isEqualTo(LocalDateTime.of(2027, 1, 1, 0, 0))
    }

    @Test
    fun `getFirstChartIntervals YEARLY returns last 6 years including current`() {
        val i18nService = mockk<I18nService>()
        every { i18nService.getMessage(any(), any(), any(), *anyVararg()) } returns "Year"
        
        val referenceDate = LocalDateTime.of(2026, 6, 15, 12, 0)
        val intervals = IntervalCalculator.getFirstChartIntervals(StatsPeriod.YEARLY, referenceDate, Language.EN, i18nService)

        assertThat(intervals).hasSize(6)
        assertThat(intervals[0].start).isEqualTo(LocalDateTime.of(2021, 1, 1, 0, 0))
        assertThat(intervals[0].end).isEqualTo(LocalDateTime.of(2022, 1, 1, 0, 0))
        assertThat(intervals[5].start).isEqualTo(LocalDateTime.of(2026, 1, 1, 0, 0))
        assertThat(intervals[5].end).isEqualTo(LocalDateTime.of(2027, 1, 1, 0, 0))
    }

    @Test
    fun `getPreviousPeriodRange DAILY returns previous week range`() {
        val periodStart = LocalDateTime.of(2026, 6, 6, 0, 0)
        val periodEnd = LocalDateTime.of(2026, 6, 13, 0, 0)

        val (prevStart, prevEnd) = IntervalCalculator.getPreviousPeriodRange(StatsPeriod.DAILY, periodStart, periodEnd)

        assertThat(prevStart).isEqualTo(LocalDateTime.of(2026, 5, 30, 0, 0))
        assertThat(prevEnd).isEqualTo(LocalDateTime.of(2026, 6, 6, 0, 0))
    }

    @Test
    fun `getPreviousPeriodRange WEEKLY returns previous month range`() {
        val periodStart = LocalDateTime.of(2026, 6, 1, 0, 0)
        val periodEnd = LocalDateTime.of(2026, 7, 1, 0, 0)

        val (prevStart, prevEnd) = IntervalCalculator.getPreviousPeriodRange(StatsPeriod.WEEKLY, periodStart, periodEnd)

        assertThat(prevStart).isEqualTo(LocalDateTime.of(2026, 5, 1, 0, 0))
        assertThat(prevEnd).isEqualTo(LocalDateTime.of(2026, 6, 1, 0, 0))
    }

    @Test
    fun `getPreviousPeriodRange MONTHLY returns previous year range`() {
        val periodStart = LocalDateTime.of(2026, 1, 1, 0, 0)
        val periodEnd = LocalDateTime.of(2027, 1, 1, 0, 0)

        val (prevStart, prevEnd) = IntervalCalculator.getPreviousPeriodRange(StatsPeriod.MONTHLY, periodStart, periodEnd)

        assertThat(prevStart).isEqualTo(LocalDateTime.of(2025, 1, 1, 0, 0))
        assertThat(prevEnd).isEqualTo(LocalDateTime.of(2026, 1, 1, 0, 0))
    }

    @Test
    fun `getPreviousPeriodRange YEARLY returns previous 6 years range`() {
        val periodStart = LocalDateTime.of(2021, 1, 1, 0, 0)
        val periodEnd = LocalDateTime.of(2027, 1, 1, 0, 0)

        val (prevStart, prevEnd) = IntervalCalculator.getPreviousPeriodRange(StatsPeriod.YEARLY, periodStart, periodEnd)

        assertThat(prevStart).isEqualTo(LocalDateTime.of(2015, 1, 1, 0, 0))
        assertThat(prevEnd).isEqualTo(LocalDateTime.of(2021, 1, 1, 0, 0))
    }

    @Test
    fun `getFirstChartIntervals WEEKLY handles February correctly`() {
        val i18nService = mockk<I18nService>()
        every { i18nService.getMessage(any(), any(), any(), *anyVararg()) } returns "W"
        
        val referenceDate = LocalDateTime.of(2026, 2, 15, 12, 0)
        val intervals = IntervalCalculator.getFirstChartIntervals(StatsPeriod.WEEKLY, referenceDate, Language.EN, i18nService)

        assertThat(intervals).hasSize(4)
        assertThat(intervals[3].end).isEqualTo(LocalDateTime.of(2026, 3, 1, 0, 0))
    }
}
