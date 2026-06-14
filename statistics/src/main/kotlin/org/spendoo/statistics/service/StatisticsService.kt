package org.spendoo.statistics.service

import org.spendoo.i18n.I18nService
import org.spendoo.statistics.api.dto.response.*
import org.spendoo.statistics.model.*
import org.spendoo.statistics.util.ChartRenderer
import org.spendoo.statistics.util.IntervalCalculator
import org.spendoo.statistics.util.PdfGenerator
import org.spendoo.storage.service.ImageStorageService
import org.spendoo.transactions.api.dto.response.CategorySpendingDto
import org.spendoo.transactions.entity.Budget
import org.spendoo.transactions.repository.BudgetRepository
import org.spendoo.transactions.repository.TransactionRepository
import org.spendoo.transactions.repository.TransactionViewRepository
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.*

//TODO: Do the analytics calculations correctly in the AI service and use it's response
@Service
class StatisticsService(
    private val transactionRepository: TransactionRepository,
    private val budgetRepository: BudgetRepository,
    private val transactionViewRepository: TransactionViewRepository,
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
        return getStatistics(userId, periodStart, periodEnd, DataType.FULL, theme, lang, imageFormat)
    }

    @Transactional(readOnly = true)
    fun getStatistics(
        userId: UUID,
        startDate: LocalDateTime,
        endDate: LocalDateTime,
        dataType: DataType,
        theme: Theme,
        lang: Language,
        imageFormat: ImageFormat
    ): StatisticsResponse {
        val days = ChronoUnit.DAYS.between(startDate.toLocalDate(), endDate.toLocalDate())
        val period = when {
            days <= 10 -> StatsPeriod.DAILY
            days in 11..42 -> StatsPeriod.WEEKLY
            days in 43..365 -> StatsPeriod.MONTHLY
            else -> StatsPeriod.YEARLY
        }

        val referenceDate = if (endDate.isAfter(LocalDateTime.now())) LocalDateTime.now() else endDate

        val firstChartIntervals = IntervalCalculator.getFirstChartIntervals(period, referenceDate, lang, i18nService)
        val periodStart = firstChartIntervals.first().start
        val periodEnd = firstChartIntervals.last().end

        // Fetch all budgets that overlap this chart's period in batches to prevent memory overflow
        val allBudgets = mutableListOf<Budget>()
        var pageNum = 0
        do {
            val page = budgetRepository.findAllBudgetsByUserIdAndDateRangeBatched(userId, periodStart, periodEnd, PageRequest.of(pageNum, 100))
            allBudgets.addAll(page.content)
            pageNum++
        } while (page.hasNext())

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
            val intervalBudget = calculateBudgetForRange(allBudgets, interval.start, interval.end)
            budgetData.add(intervalBudget.setScale(2, RoundingMode.HALF_UP))

            val intervalEndLimit = if (referenceDate.isBefore(interval.end)) referenceDate else interval.end
            val intervalSpent = getPeriodAmount(userId, interval.start, intervalEndLimit, dataType)
            spentData.add(intervalSpent.setScale(2, RoundingMode.HALF_UP))
        }

        val maxSpentValue = spentData.maxOrNull() ?: BigDecimal.ZERO
        val maxSpentPosition = if (spentData.isNotEmpty()) {
            spentData.indexOf(maxSpentValue).takeIf { it >= 0 } ?: 0
        } else {
            0
        }

        if (currentIntervalIdx in firstChartIntervals.indices) {
            for (i in 0 until currentIntervalIdx) {
                forecastData.add(BigDecimal.ZERO)
            }
            
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
        
        val last6Budgets = mutableListOf<Budget>()
        var pageNum6 = 0
        do {
            val page = budgetRepository.findAllBudgetsByUserIdAndDateRangeBatched(userId, startLast6, endLast6, PageRequest.of(pageNum6, 100))
            last6Budgets.addAll(page.content)
            pageNum6++
        } while (page.hasNext())

        for (interval in secondChartIntervals) {
            val periodSpent = getPeriodAmount(userId, interval.start, interval.end, dataType)
            val periodBudget = calculateBudgetForRange(last6Budgets, interval.start, interval.end)

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

        val currentPeriodSpent = getPeriodAmount(userId, periodStart, periodEnd, dataType)

        val categorySpendingList = getTopCategories(userId, periodStart, periodEnd, dataType)

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
        val prevCategorySpending = getTopCategories(userId, prevStart, prevEnd, dataType)

        val prevSpendingMap = prevCategorySpending.associate { it.categoryName to it.totalAmount.abs() }

        val topCategoryDtos = categorySpendingList.map { dto ->
            val catName = dto.categoryName
            val curAmt = dto.totalAmount.abs()
            val prevAmt = prevSpendingMap[catName] ?: BigDecimal.ZERO

            val (pctChange, trend) = calculatePercentageChange(curAmt, prevAmt)

            TopCategoryDto(
                categoryName = catName.lowercase(),
                categoryIcon = dto.categoryIcon,
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
        startDate: LocalDateTime,
        endDate: LocalDateTime,
        reportType: ReportType,
        dataType: DataType,
        theme: Theme,
        lang: Language
    ): ByteArray {
        return if (reportType == ReportType.CHARTS) {
            val stats = getStatistics(userId, startDate, endDate, dataType, theme, lang, ImageFormat.BASE64)
            val days = ChronoUnit.DAYS.between(startDate.toLocalDate(), endDate.toLocalDate())
            val period = when {
                days <= 10 -> StatsPeriod.DAILY
                days in 11..42 -> StatsPeriod.WEEKLY
                days in 43..365 -> StatsPeriod.MONTHLY
                else -> StatsPeriod.YEARLY
            }
            PdfGenerator.generatePdf(stats, period, theme, lang, i18nService)
        } else {
            PdfGenerator.generateDetailedPdf(
                userId = userId,
                startDate = startDate,
                endDate = endDate,
                dataType = dataType,
                theme = theme,
                lang = lang,
                i18nService = i18nService,
                transactionRepository = transactionRepository,
                transactionViewRepository = transactionViewRepository,
                budgetRepository = budgetRepository
            )
        }
    }

    private fun getPeriodAmount(userId: UUID, start: LocalDateTime, end: LocalDateTime, dataType: DataType): BigDecimal {
        return when(dataType) {
            DataType.EXPENSES -> transactionRepository.sumExpensesByUserIdAndDateRange(userId, start, end).negate()
            DataType.INCOME -> transactionRepository.sumIncomeByUserIdAndDateRange(userId, start, end)
            DataType.FULL -> {
                val exp = transactionRepository.sumExpensesByUserIdAndDateRange(userId, start, end).negate()
                val inc = transactionRepository.sumIncomeByUserIdAndDateRange(userId, start, end)
                exp + inc
            }
        }
    }

    private fun getTopCategories(userId: UUID, start: LocalDateTime, end: LocalDateTime, dataType: DataType): List<CategorySpendingDto> {
        return when(dataType) {
            DataType.EXPENSES -> transactionRepository.findTopSpendingCategoriesInDateRange(userId, start, end)
            DataType.INCOME -> transactionRepository.findTopIncomeCategoriesInDateRange(userId, start, end)
            DataType.FULL -> transactionRepository.findTopAllCategoriesInDateRange(userId, start, end)
        }
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

    private fun calculateBudgetForRange(budgets: List<Budget>, start: LocalDateTime, end: LocalDateTime): BigDecimal {
        // Find all budgets whose range includes this interval (overlaps)
        val overlappingBudgets = budgets.filter {
            it.startDate < end && it.endDate > start
        }

        // Group by categoryId, and for each category, take the newest budget (by startDate)
        val groupedByCategory = overlappingBudgets.groupBy { it.category.id }
        
        var totalBudget = BigDecimal.ZERO
        for ((_, categoryBudgets) in groupedByCategory) {
            val newestBudget = categoryBudgets.maxByOrNull { it.startDate }
            if (newestBudget != null) {
                totalBudget = totalBudget.add(newestBudget.amount)
            }
        }

        return totalBudget
    }
}
