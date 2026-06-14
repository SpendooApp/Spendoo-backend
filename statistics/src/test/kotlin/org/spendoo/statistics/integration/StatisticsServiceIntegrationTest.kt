package org.spendoo.statistics.integration

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.spendoo.statistics.StatisticsTestApplication
import org.spendoo.statistics.model.StatsPeriod
import org.spendoo.statistics.model.TrendDirection
import org.spendoo.statistics.model.Theme
import org.spendoo.statistics.model.Language
import org.spendoo.statistics.model.ImageFormat
import org.spendoo.statistics.model.BarStatus
import org.spendoo.statistics.service.StatisticsService
import org.spendoo.transactions.entity.*
import org.spendoo.transactions.repository.BudgetRepository
import org.spendoo.transactions.repository.CategoryRepository
import org.spendoo.transactions.repository.TransactionRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.*

@SpringBootTest(classes = [StatisticsTestApplication::class])
@ActiveProfiles("test")
class StatisticsServiceIntegrationTest {

    @Autowired
    private lateinit var statisticsService: StatisticsService

    @Autowired
    private lateinit var categoryRepository: CategoryRepository

    @Autowired
    private lateinit var budgetRepository: BudgetRepository

    @Autowired
    private lateinit var transactionRepository: TransactionRepository

    private lateinit var userId: UUID
    private lateinit var foodCategory: Category
    private lateinit var shoppingCategory: Category

    @BeforeEach
    fun setUp() {
        transactionRepository.deleteAll()
        budgetRepository.deleteAll()
        categoryRepository.deleteAll()

        userId = UUID.randomUUID()

        foodCategory = categoryRepository.save(
            Category(
                userId = userId,
                categoryName = "Food",
                categoryIcon = CategoryIcon.FOOD,
                leftOverOptions = LeftOverOptions.RESET_TO_ORIGINAL_AMOUNT,
                priority = 2
            )
        )

        shoppingCategory = categoryRepository.save(
            Category(
                userId = userId,
                categoryName = "Shopping",
                categoryIcon = CategoryIcon.SHOPPING,
                leftOverOptions = LeftOverOptions.RESET_TO_ORIGINAL_AMOUNT,
                priority = 2
            )
        )
    }

    @Test
    fun `getStatistics DAILY returns correct charts and top categories`() {
        // Daily x-axis is Saturday to Friday of current week.
        // Let's set reference date to Thursday June 11, 2026.
        val referenceDate = LocalDateTime.of(2026, 6, 11, 12, 0, 0)
        
        // Saturday of this week is June 6, 2026.
        // Friday of this week is June 12, 2026.
        val sat = LocalDateTime.of(2026, 6, 6, 0, 0)
        val sun = sat.plusDays(1)
        val mon = sat.plusDays(2)
        val tue = sat.plusDays(3)
        val wed = sat.plusDays(4)
        val thu = sat.plusDays(5) // referenceDate is in this interval
        val fri = sat.plusDays(6)

        // Setup active budget for food: 300.0 from June 1 to June 30 (30 days) -> 10.0 per day
        budgetRepository.save(
            Budget(
                amount = BigDecimal("300.00"),
                carryOver = BigDecimal.ZERO,
                period = 30,
                startDate = LocalDateTime.of(2026, 6, 1, 0, 0),
                endDate = LocalDateTime.of(2026, 7, 1, 0, 0),
                isActive = true,
                category = foodCategory
            )
        )

        // Setup transaction on Sunday (June 7) and Tuesday (June 9)
        transactionRepository.save(
            Transaction(
                userId = userId,
                title = "Sunday Snack",
                amount = BigDecimal("-15.00"),
                note = "snack",
                transactionDate = sun.plusHours(12),
                category = foodCategory
            )
        )

        transactionRepository.save(
            Transaction(
                userId = userId,
                title = "Tuesday Lunch",
                amount = BigDecimal("-35.00"),
                note = "lunch",
                transactionDate = tue.plusHours(12),
                category = foodCategory
            )
        )

        // Previous week transactions for indicator check: previous week is May 30 (Sat) to June 5 (Fri)
        transactionRepository.save(
            Transaction(
                userId = userId,
                title = "Prev Week Lunch",
                amount = BigDecimal("-20.00"),
                note = "prev lunch",
                transactionDate = sat.minusDays(3), // Wednesday June 3
                category = foodCategory
            )
        )

        val stats = statisticsService.getStatistics(userId, referenceDate.minusDays(10), referenceDate, org.spendoo.statistics.model.DataType.FULL, Theme.LIGHT, Language.EN, ImageFormat.BASE64)

        // 1. Line Chart validation
        assertThat(stats.lineChart.labels).containsExactly("Sat", "Sun", "Mon", "Tue", "Wed", "Thu", "Fri")
        assertThat(stats.lineChart.budgetData[0].toDouble()).isWithin(0.1).of(300.0)
        assertThat(stats.lineChart.spentData[1].toDouble()).isWithin(0.1).of(15.0)
        assertThat(stats.lineChart.spentData[3].toDouble()).isWithin(0.1).of(35.0)
        assertThat(stats.lineChart.spentData[4].toDouble()).isEqualTo(0.0)
        assertThat(stats.lineChart.maxSpentValue.toDouble()).isWithin(0.1).of(35.0)
        assertThat(stats.lineChart.maxSpentPosition).isEqualTo(3)
        assertThat(stats.lineChart.spentData).hasSize(6)
        assertThat(stats.lineChart.forecastData).hasSize(7)

        // 2. Bar Chart validation (Last 6 Days)
        assertThat(stats.barChart.data).hasSize(6)
        val todayBar = stats.barChart.data.last()
        assertThat(todayBar.periodLabel).isEqualTo("Thu")

        // 3. Donut Chart validation
        assertThat(stats.donutChart.totalSpent.toDouble()).isWithin(0.1).of(50.0) // 15.0 + 35.0
        assertThat(stats.donutChart.data).hasSize(1)
        assertThat(stats.donutChart.data.first().categoryName).isEqualTo("food")
        assertThat(stats.donutChart.data.first().amount.toDouble()).isWithin(0.1).of(50.0)

        // 4. Top Category indicator validation
        // Current Food Spent: 50.0, Previous Food Spent: 20.0
        // Change: (50 - 20) / 20 * 100 = 150%
        val foodDto = stats.topCategories.firstOrNull { it.categoryName == "food" }
        assertThat(foodDto).isNotNull()
        assertThat(foodDto?.percentageChange?.toDouble()).isWithin(0.1).of(150.0)
        assertThat(foodDto?.trend).isEqualTo(TrendDirection.UP)

        // Image data check
        assertThat(stats.lineChart.image).startsWith("data:image/png;base64,")
        assertThat(stats.barChart.image).startsWith("data:image/png;base64,")
        assertThat(stats.donutChart.image).startsWith("data:image/png;base64,")
    }

    @Test
    fun `getStatistics MONTHLY returns arabic labels and dark theme images`() {
        val referenceDate = LocalDateTime.of(2026, 6, 11, 12, 0, 0)
        val stats = statisticsService.getStatistics(userId, referenceDate.minusDays(50), referenceDate, org.spendoo.statistics.model.DataType.FULL, Theme.DARK, Language.AR, ImageFormat.BASE64)

        assertThat(stats.lineChart.labels).contains("يونيو")
        assertThat(stats.lineChart.image).startsWith("data:image/png;base64,")
    }

    @Test
    fun `getStatisticsPdf returns valid pdf bytes`() {
        val referenceDate = LocalDateTime.of(2026, 6, 11, 12, 0, 0)
        val pdfBytes = statisticsService.getStatisticsPdf(userId, referenceDate.minusDays(10), referenceDate, org.spendoo.statistics.model.ReportType.CHARTS, org.spendoo.statistics.model.DataType.FULL, Theme.LIGHT, Language.EN)

        assertThat(pdfBytes).isNotEmpty()
        assertThat(pdfBytes[0].toInt()).isEqualTo(0x25)
        assertThat(pdfBytes[1].toInt()).isEqualTo(0x50)
        assertThat(pdfBytes[2].toInt()).isEqualTo(0x44)
        assertThat(pdfBytes[3].toInt()).isEqualTo(0x46)
    }

    @Test
    fun `getStatistics bar chart handles zero budget with spending as overspending`() {
        val referenceDate = LocalDateTime.of(2026, 6, 11, 12, 0, 0)
        
        transactionRepository.save(
            Transaction(
                userId = userId,
                title = "Shopping without budget",
                amount = BigDecimal("-100.00"),
                note = "no budget set",
                transactionDate = LocalDateTime.of(2026, 6, 7, 12, 0),
                category = shoppingCategory
            )
        )

        val stats = statisticsService.getStatistics(userId, referenceDate.minusDays(10), referenceDate, org.spendoo.statistics.model.DataType.FULL, Theme.LIGHT, Language.EN, ImageFormat.BASE64)

        val dayWithSpending = stats.barChart.data.find { it.spent > BigDecimal.ZERO }
        assertThat(dayWithSpending).isNotNull()
        assertThat(dayWithSpending?.budget?.compareTo(BigDecimal.ZERO)).isEqualTo(0)
        assertThat(dayWithSpending?.ratio?.toDouble()).isWithin(0.1).of(1.1)
        assertThat(dayWithSpending?.status).isEqualTo(BarStatus.OVERSPENDING)
    }

    @Test
    fun `getStatistics line chart does not produce negative indices`() {
        val referenceDate = LocalDateTime.of(2026, 6, 11, 12, 0, 0)
        
        budgetRepository.save(
            Budget(
                amount = BigDecimal("500.00"),
                carryOver = BigDecimal.ZERO,
                period = 30,
                startDate = LocalDateTime.of(2026, 6, 1, 0, 0),
                endDate = LocalDateTime.of(2026, 7, 1, 0, 0),
                isActive = true,
                category = foodCategory
            )
        )

        for (i in 0..5) {
            transactionRepository.save(
                Transaction(
                    userId = userId,
                    title = "Day $i expense",
                    amount = BigDecimal("-${10 + i * 5}.00"),
                    note = "test",
                    transactionDate = LocalDateTime.of(2026, 6, 6 + i, 12, 0),
                    category = foodCategory
                )
            )
        }

        val stats = statisticsService.getStatistics(userId, referenceDate.minusDays(10), referenceDate, org.spendoo.statistics.model.DataType.FULL, Theme.LIGHT, Language.EN, ImageFormat.BASE64)

        assertThat(stats.lineChart.spentData).hasSize(6)
        assertThat(stats.lineChart.budgetData).hasSize(6)
        stats.lineChart.spentData.forEach { spent ->
            assertThat(spent.toDouble()).isAtLeast(0.0)
        }
    }

    @Test
    fun `getStatistics budget calculation handles overlapping categories and newer budgets correctly`() {
        val referenceDate = LocalDateTime.of(2026, 6, 11, 12, 0, 0)
        
        // Category 1: Old budget, superseded by new budget in June
        val oldBudget = budgetRepository.save(
            Budget(
                amount = BigDecimal("1000.00"),
                carryOver = BigDecimal.ZERO,
                period = 30,
                startDate = LocalDateTime.of(2026, 5, 20, 0, 0),
                endDate = LocalDateTime.of(2026, 6, 20, 0, 0),
                isActive = false,
                category = foodCategory
            )
        )
        // Simulate an update on June 5th, so the new budget starts then
        val newBudget = budgetRepository.save(
            Budget(
                amount = BigDecimal("1500.00"),
                carryOver = BigDecimal.ZERO,
                period = 30,
                startDate = LocalDateTime.of(2026, 6, 5, 0, 0),
                endDate = LocalDateTime.of(2026, 7, 5, 0, 0),
                isActive = true,
                category = foodCategory
            )
        )

        // Category 2: A different category budget overlapping the same period
        budgetRepository.save(
            Budget(
                amount = BigDecimal("500.00"),
                carryOver = BigDecimal.ZERO,
                period = 30,
                startDate = LocalDateTime.of(2026, 6, 1, 0, 0),
                endDate = LocalDateTime.of(2026, 7, 1, 0, 0),
                isActive = true,
                category = shoppingCategory
            )
        )

        val stats = statisticsService.getStatistics(userId, StatsPeriod.MONTHLY, Theme.LIGHT, Language.EN, ImageFormat.BASE64, referenceDate)

        // For MONTHLY, intervals are Jan, Feb, Mar, Apr, May, Jun...
        // May budget:
        // Food: overlaps May 20-Jun 20 (amount 1000). (New budget starts Jun 5, doesn't overlap May).
        // Shopping: overlaps Jun 1-Jul 1 (doesn't overlap May).
        // May total = 1000.
        
        // Jun budget:
        // Food: overlaps May 20-Jun 20 (amount 1000) AND Jun 5-Jul 5 (amount 1500). 
        // User rule: "if the categor budget has 2 values in the month take the seconed one (the newer)."
        // Newer by startDate is 1500.
        // Shopping: overlaps Jun 1-Jul 1 (amount 500).
        // Jun total = 1500 + 500 = 2000.

        val labels = stats.lineChart.labels
        val mayIndex = labels.indexOf("May")
        val junIndex = labels.indexOf("Jun")

        assertThat(stats.lineChart.budgetData[mayIndex].toDouble()).isWithin(0.1).of(1000.0)
        assertThat(stats.lineChart.budgetData[junIndex].toDouble()).isWithin(0.1).of(2000.0)
    }

    @Test
    fun `getStatistics forecast data aligns correctly with current interval`() {
        val referenceDate = LocalDateTime.of(2026, 6, 11, 12, 0, 0)
        
        budgetRepository.save(
            Budget(
                amount = BigDecimal("700.00"),
                carryOver = BigDecimal.ZERO,
                period = 30,
                startDate = LocalDateTime.of(2026, 6, 1, 0, 0),
                endDate = LocalDateTime.of(2026, 7, 1, 0, 0),
                isActive = true,
                category = foodCategory
            )
        )

        transactionRepository.save(
            Transaction(
                userId = userId,
                title = "Recent expense",
                amount = BigDecimal("-50.00"),
                note = "test",
                transactionDate = LocalDateTime.of(2026, 6, 10, 12, 0),
                category = foodCategory
            )
        )

        val stats = statisticsService.getStatistics(userId, referenceDate.minusDays(10), referenceDate, org.spendoo.statistics.model.DataType.FULL, Theme.LIGHT, Language.EN, ImageFormat.BASE64)

        assertThat(stats.lineChart.forecastData).isNotEmpty()
        assertThat(stats.lineChart.forecastData.first().toDouble()).isAtLeast(0.0)
        
        val lastSpent = stats.lineChart.spentData.lastOrNull()
        if (lastSpent != null && stats.lineChart.forecastData.isNotEmpty()) {
            val idx = stats.lineChart.spentData.size - 1
            assertThat(stats.lineChart.forecastData[idx]).isEqualTo(lastSpent)
        }
    }

    @Test
    fun `getStatistics with multiple categories returns correct donut chart slices`() {
        val referenceDate = LocalDateTime.of(2026, 6, 11, 12, 0, 0)

        transactionRepository.save(
            Transaction(
                userId = userId,
                title = "Food expense",
                amount = BigDecimal("-300.00"),
                note = "food",
                transactionDate = LocalDateTime.of(2026, 6, 7, 12, 0),
                category = foodCategory
            )
        )

        transactionRepository.save(
            Transaction(
                userId = userId,
                title = "Shopping expense",
                amount = BigDecimal("-200.00"),
                note = "shopping",
                transactionDate = LocalDateTime.of(2026, 6, 8, 12, 0),
                category = shoppingCategory
            )
        )

        val stats = statisticsService.getStatistics(userId, referenceDate.minusDays(10), referenceDate, org.spendoo.statistics.model.DataType.FULL, Theme.LIGHT, Language.EN, ImageFormat.BASE64)

        assertThat(stats.donutChart.data).hasSize(2)
        assertThat(stats.donutChart.totalSpent.toDouble()).isWithin(0.1).of(500.0)
        
        val foodSlice = stats.donutChart.data.find { it.categoryName == "food" }
        val shoppingSlice = stats.donutChart.data.find { it.categoryName == "shopping" }
        
        assertThat(foodSlice?.percentage?.toDouble()).isWithin(0.1).of(60.0)
        assertThat(shoppingSlice?.percentage?.toDouble()).isWithin(0.1).of(40.0)
    }

    @Test
    fun `getStatistics previous period calculation uses correct date range`() {
        val referenceDate = LocalDateTime.of(2026, 6, 11, 12, 0, 0)
        
        transactionRepository.save(
            Transaction(
                userId = userId,
                title = "Current week",
                amount = BigDecimal("-100.00"),
                note = "current",
                transactionDate = LocalDateTime.of(2026, 6, 8, 12, 0),
                category = foodCategory
            )
        )

        transactionRepository.save(
            Transaction(
                userId = userId,
                title = "Previous week",
                amount = BigDecimal("-50.00"),
                note = "previous",
                transactionDate = LocalDateTime.of(2026, 6, 1, 12, 0),
                category = foodCategory
            )
        )

        val stats = statisticsService.getStatistics(userId, referenceDate.minusDays(10), referenceDate, org.spendoo.statistics.model.DataType.FULL, Theme.LIGHT, Language.EN, ImageFormat.BASE64)

        val foodDto = stats.topCategories.find { it.categoryName == "food" }
        assertThat(foodDto).isNotNull()
        assertThat(foodDto?.percentageChange?.toDouble()).isWithin(0.1).of(100.0)
        assertThat(foodDto?.trend).isEqualTo(TrendDirection.UP)
    }
}

