package org.spendoo.statistics.service

import org.spendoo.i18n.I18nService
import org.spendoo.statistics.api.dto.response.*
import org.spendoo.statistics.model.*
import org.spendoo.statistics.util.ChartRenderer
import org.spendoo.statistics.util.IntervalCalculator
import org.spendoo.statistics.util.PdfGenerator
import org.spendoo.storage.service.ImageStorageService
import org.spendoo.transactions.api.dto.response.BudgetIntervalDto
import org.spendoo.transactions.entity.CategoryIcon
import org.spendoo.transactions.repository.BudgetRepository
import org.spendoo.transactions.repository.TransactionRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.*

@Service
class StatisticsService(
    private val transactionRepository: TransactionRepository,
    private val budgetRepository: BudgetRepository,
    private val i18nService: I18nService,
    private val imageStorageService: ImageStorageService
) {

    @Transactional(readOnly = true)
    fun getStatistics(
        userId: UUID,
        period: StatsPeriod,
        theme: Theme,
        lang: Language,
        imageFormat: ImageFormat,
        referenceDate: LocalDateTime = LocalDateTime.now()
    ): StatisticsResponse {
        val firstChartIntervals = IntervalCalculator.getFirstChartIntervals(period, referenceDate, lang, i18nService)
        val periodStart = firstChartIntervals.first().start
        val periodEnd = firstChartIntervals.last().end

        val budgets = budgetRepository.findAllBudgetsByUserIdAndDateRange(userId, periodStart, periodEnd)

        val firstChartLabels = firstChartIntervals.map { it.label }
        val budgetData = mutableListOf<BigDecimal>()
        val spentData = mutableListOf<BigDecimal>()
        val forecastData = mutableListOf<BigDecimal>()

        var currentIntervalIdx = -1
        for (i in firstChartIntervals.indices) {
            val interval = firstChartIntervals[i]
            if ((referenceDate.isAfter(interval.start) || referenceDate.isEqual(interval.start)) && referenceDate.isBefore(
                    interval.end
                )) {
                currentIntervalIdx = i
                break
            }
        }
        if (currentIntervalIdx == -1) {
            currentIntervalIdx = if (referenceDate.isAfter(periodEnd)) firstChartIntervals.size - 1 else 0
        }

        for (i in 0..currentIntervalIdx) {
            val interval = firstChartIntervals[i]
            val intervalBudget = calculateBudgetForRange(budgets, interval.start, interval.end)
            budgetData.add(intervalBudget.setScale(2, RoundingMode.HALF_UP))

            val intervalEndLimit = if (referenceDate.isBefore(interval.end)) referenceDate else interval.end
            val intervalSpent = transactionRepository.sumExpensesByUserIdAndDateRange(userId, interval.start, intervalEndLimit).negate()
            spentData.add(intervalSpent.setScale(2, RoundingMode.HALF_UP))
        }

        val maxSpentValue = spentData.maxOrNull() ?: BigDecimal.ZERO
        val maxSpentPosition = if (spentData.isNotEmpty()) {
            spentData.indexOf(maxSpentValue).takeIf { it >= 0 } ?: 0
        } else {
            0
        }

        if (currentIntervalIdx in firstChartIntervals.indices) {
            val spentSoFar = spentData.lastOrNull() ?: BigDecimal.ZERO
            forecastData.add(spentSoFar)

            val avgBudget = budgetData.firstOrNull { it > BigDecimal.ZERO } ?: BigDecimal(1000.0)

            for (i in (currentIntervalIdx + 1) until firstChartIntervals.size) {
                val step = i - currentIntervalIdx
                val mockForecastVal = spentSoFar + avgBudget.multiply(BigDecimal.valueOf(step * 0.15))
                forecastData.add(mockForecastVal.setScale(2, RoundingMode.HALF_UP))
            }
        }

        val firstChartImgBytes = ChartRenderer.renderLineChart(
            labels = firstChartLabels,
            budgetData = budgetData,
            spentData = spentData,
            forecastData = forecastData,
            forecastStartIdx = currentIntervalIdx,
            theme = theme,
            lang = lang,
            i18nService = i18nService
        )

        val firstChartImg = processImage(firstChartImgBytes, imageFormat, "line_chart_${UUID.randomUUID()}")

        val lineChart = LineChartData(
            labels = firstChartLabels,
            budgetData = budgetData,
            spentData = spentData,
            forecastData = forecastData,
            maxSpentValue = maxSpentValue,
            maxSpentPosition = maxSpentPosition,
            image = firstChartImg
        )

        val secondChartIntervals = IntervalCalculator.getSecondChartIntervals(period, referenceDate, lang, i18nService)
        val barChartLabels = secondChartIntervals.map { it.label }
        val barItems = mutableListOf<BarChartPeriodItem>()

        val startLast6 = secondChartIntervals.first().start
        val endLast6 = secondChartIntervals.last().end
        val budgetsLast6 = budgetRepository.findAllBudgetsByUserIdAndDateRange(userId, startLast6, endLast6)

        for (interval in secondChartIntervals) {
            val periodSpent = transactionRepository.sumExpensesByUserIdAndDateRange(userId, interval.start, interval.end).negate()
            val periodBudget = calculateBudgetForRange(budgetsLast6, interval.start, interval.end)

            val ratio = if (periodBudget.compareTo(BigDecimal.ZERO) == 0) {
                if (periodSpent.compareTo(BigDecimal.ZERO) == 0) BigDecimal.ZERO else BigDecimal("1.1")
            } else {
                periodSpent.divide(periodBudget, 4, RoundingMode.HALF_UP)
            }

            val status = when {
                ratio < BigDecimal("0.9") -> BarStatus.WITHIN
                ratio <= BigDecimal("1.0") -> BarStatus.RISK
                else -> BarStatus.OVERSPENDING
            }

            barItems.add(
                BarChartPeriodItem(
                    periodLabel = interval.label,
                    spent = periodSpent.setScale(2, RoundingMode.HALF_UP),
                    budget = periodBudget.setScale(2, RoundingMode.HALF_UP),
                    ratio = ratio.setScale(4, RoundingMode.HALF_UP),
                    status = status
                )
            )
        }

        val secondChartImgBytes = ChartRenderer.renderBarChart(
            labels = barChartLabels,
            budgetData = barItems.map { it.budget },
            spentData = barItems.map { it.spent },
            theme = theme,
            lang = lang,
            i18nService = i18nService
        )

        val secondChartImg = processImage(secondChartImgBytes, imageFormat, "bar_chart_${UUID.randomUUID()}")

        val barChart = BarChartData(
            labels = barChartLabels,
            data = barItems,
            image = secondChartImg
        )

        val currentPeriodSpent = transactionRepository.sumExpensesByUserIdAndDateRange(userId, periodStart, periodEnd).negate()

        val categorySpendingList = transactionRepository.findTopSpendingCategoriesInDateRange(userId, periodStart, periodEnd)

        val donutSlices = mutableListOf<DonutChartSlice>()
        val renderCategoryNames = mutableListOf<String>()
        val renderCategoryAmounts = mutableListOf<BigDecimal>()

        val totalCategorySpent = categorySpendingList.fold(BigDecimal.ZERO) { acc, dto -> acc + dto.totalAmount.abs() }

        if (categorySpendingList.isNotEmpty()) {
            val top5 = categorySpendingList.take(5)
            val rest = categorySpendingList.drop(5)

            for (dto in top5) {
                val amt = dto.totalAmount.abs()
                val pct = if (totalCategorySpent > BigDecimal.ZERO) {
                    amt.divide(totalCategorySpent, 4, RoundingMode.HALF_UP).multiply(BigDecimal(100.0))
                } else BigDecimal.ZERO

                val translatedName = dto.categoryName.lowercase()
                donutSlices.add(
                    DonutChartSlice(
                        categoryName = translatedName,
                        amount = amt.setScale(2, RoundingMode.HALF_UP),
                        percentage = pct.setScale(1, RoundingMode.HALF_UP)
                    )
                )
                renderCategoryNames.add(translatedName)
                renderCategoryAmounts.add(amt)
            }

            if (rest.isNotEmpty()) {
                val restAmt = rest.fold(BigDecimal.ZERO) { acc, dto -> acc + dto.totalAmount.abs() }
                val pct = if (totalCategorySpent > BigDecimal.ZERO) {
                    restAmt.divide(totalCategorySpent, 4, RoundingMode.HALF_UP).multiply(BigDecimal(100.0))
                } else BigDecimal.ZERO

                val otherLabel = i18nService.getMessage("other", lang.locale)
                donutSlices.add(
                    DonutChartSlice(
                        categoryName = otherLabel,
                        amount = restAmt.setScale(2, RoundingMode.HALF_UP),
                        percentage = pct.setScale(1, RoundingMode.HALF_UP)
                    )
                )
                renderCategoryNames.add(otherLabel)
                renderCategoryAmounts.add(restAmt)
            }
        }

        val donutChartImgBytes = ChartRenderer.renderDonutChart(
            totalSpent = currentPeriodSpent.setScale(2, RoundingMode.HALF_UP),
            categoryNames = renderCategoryNames,
            categoryAmounts = renderCategoryAmounts,
            theme = theme,
            lang = lang,
            i18nService = i18nService
        )

        val donutChartImg = processImage(donutChartImgBytes, imageFormat, "donut_chart_${UUID.randomUUID()}")

        val donutChart = DonutChartData(
            totalSpent = currentPeriodSpent.setScale(2, RoundingMode.HALF_UP),
            data = donutSlices,
            image = donutChartImg
        )

        val (prevStart, prevEnd) = IntervalCalculator.getPreviousPeriodRange(period, periodStart, periodEnd)
        val prevCategorySpending = transactionRepository.findTopSpendingCategoriesInDateRange(userId, prevStart, prevEnd)

        val prevSpendingMap = prevCategorySpending.associate { it.categoryName to it.totalAmount.abs() }
        val curSpendingMap = categorySpendingList.associate { it.categoryName to it.totalAmount.abs() }

        val allCategoryNames = (curSpendingMap.keys + prevSpendingMap.keys).toSet()

        val iconMap = (categorySpendingList + prevCategorySpending).associate { it.categoryName to it.categoryIcon }

        val topCategoryDtos = allCategoryNames.map { catName ->
            val curAmt = curSpendingMap[catName] ?: BigDecimal.ZERO
            val prevAmt = prevSpendingMap[catName] ?: BigDecimal.ZERO

            val (pctChange, trend) = calculatePercentageChange(curAmt, prevAmt)
            val icon = iconMap[catName] ?: CategoryIcon.DEFAULT

            TopCategoryDto(
                categoryName = catName.lowercase(),
                categoryIcon = icon,
                amount = curAmt.setScale(2, RoundingMode.HALF_UP),
                percentageChange = pctChange.setScale(1, RoundingMode.HALF_UP),
                trend = trend
            )
        }.sortedByDescending { it.amount }

        return StatisticsResponse(
            lineChart = lineChart,
            barChart = barChart,
            donutChart = donutChart,
            topCategories = topCategoryDtos
        )
    }

    @Transactional(readOnly = true)
    fun getStatisticsPdf(
        userId: UUID,
        period: StatsPeriod,
        theme: Theme,
        lang: Language,
        referenceDate: LocalDateTime = LocalDateTime.now()
    ): ByteArray {
        val stats = getStatistics(userId, period, theme, lang, ImageFormat.BASE64, referenceDate)
        return PdfGenerator.generatePdf(stats, period, theme, lang, i18nService)
    }

    private fun processImage(bytes: ByteArray, format: ImageFormat, fileName: String): String {
        return if (format == ImageFormat.URL) {
            imageStorageService.uploadImageFromBytes(bytes, "image/png", fileName, "statistics")
        } else {
            "data:image/png;base64," + Base64.getEncoder().encodeToString(bytes)
        }
    }

    private fun calculatePercentageChange(current: BigDecimal, previous: BigDecimal): Pair<BigDecimal, TrendDirection> {
        if (previous.compareTo(BigDecimal.ZERO) == 0) {
            return if (current.compareTo(BigDecimal.ZERO) == 0) {
                Pair(BigDecimal.ZERO, TrendDirection.FLAT)
            } else {
                Pair(BigDecimal("100.00"), TrendDirection.UP)
            }
        }
        val diff = current - previous
        val change = diff.divide(previous, 4, RoundingMode.HALF_UP).multiply(BigDecimal("100.00"))
        return when {
            change > BigDecimal.ZERO -> Pair(change, TrendDirection.UP)
            change < BigDecimal.ZERO -> Pair(change.abs(), TrendDirection.DOWN)
            else -> Pair(BigDecimal.ZERO, TrendDirection.FLAT)
        }
    }

    private fun calculateBudgetForRange(budgets: List<BudgetIntervalDto>, start: LocalDateTime, end: LocalDateTime): BigDecimal {
        var totalBudget = BigDecimal.ZERO
        val rangeSeconds = ChronoUnit.SECONDS.between(start, end)
        if (rangeSeconds <= 0) return BigDecimal.ZERO

        for (b in budgets) {
            val overlapStart = if (b.startDate.isAfter(start)) b.startDate else start
            val overlapEnd = if (b.endDate.isBefore(end)) b.endDate else end

            if (overlapStart.isBefore(overlapEnd)) {
                val overlapSeconds = ChronoUnit.SECONDS.between(overlapStart, overlapEnd)
                val budgetSeconds = ChronoUnit.SECONDS.between(b.startDate, b.endDate)
                if (budgetSeconds > 0) {
                    val fraction = overlapSeconds.toDouble() / budgetSeconds.toDouble()
                    val allocated = b.amount.multiply(BigDecimal.valueOf(fraction))
                    totalBudget = totalBudget.add(allocated)
                }
            }
        }
        return totalBudget
    }
}
