package org.spendoo.statistics.integration

import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.spendoo.client.ApiClient
import org.spendoo.statistics.StatisticsTestApplication
import org.spendoo.statistics.api.dto.response.BudgetStatusResponse
import org.spendoo.statistics.api.dto.response.CombinedStatsResponse
import org.spendoo.statistics.api.dto.response.FinancialStatsResponse
import org.spendoo.statistics.api.dto.response.TopCategoriesResponse
import org.spendoo.statistics.model.Granularity
import org.spendoo.statistics.model.ReportDataType
import org.spendoo.statistics.model.Theme
import org.spendoo.statistics.service.StatisticsService
import org.spendoo.transactions.entity.Category
import org.spendoo.transactions.entity.CategoryIcon
import org.spendoo.transactions.entity.LeftOverOptions
import org.spendoo.transactions.entity.Transaction
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

    @Autowired
    private lateinit var apiClient: ApiClient

    private lateinit var userId: UUID
    private lateinit var foodCategory: Category

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
    }

    @Test
    fun `getStatistics calls AI service combined endpoint`() {
        val granularity = Granularity.MONTH
        val startDate = LocalDateTime.of(2026, 6, 1, 0, 0)
        val endDate = LocalDateTime.of(2026, 7, 1, 0, 0)

        val mockResponse = CombinedStatsResponse(
            financialStats = FinancialStatsResponse(emptyList(), 0, BigDecimal.ZERO, false),
            budgetStatus = BudgetStatusResponse(emptyList(), BigDecimal.ZERO),
            topCategories = TopCategoriesResponse(BigDecimal.ZERO, emptyList())
        )

        every {
            apiClient.call(CombinedStatsResponse::class.java, any())
        } returns mockResponse

        val result = statisticsService.getStatistics(userId, granularity, startDate, endDate)

        assertThat(result).isEqualTo(mockResponse)

        verify(exactly = 1) {
            apiClient.call(CombinedStatsResponse::class.java, any())
        }
    }

    @Test
    fun `getStatisticsPdf returns valid pdf bytes`() {
        val referenceDate = LocalDateTime.of(2026, 6, 11, 12, 0, 0)

        every {
            apiClient.call(Map::class.java, any())
        } returns mapOf("totalSaved" to 100.0)
        
        // Add a transaction so the detailed report is generated with some data
        transactionRepository.save(
            Transaction(
                userId = userId,
                title = "Dinner",
                amount = BigDecimal("-35.00"),
                note = "snack",
                transactionDate = referenceDate.minusDays(2),
                category = foodCategory
            )
        )

        val pdfBytes = statisticsService.getStatisticsPdf(
            userId = userId,
            startDate = referenceDate.minusDays(10),
            endDate = referenceDate,
            reportDataType = ReportDataType.FULL,
            theme = Theme.LIGHT,
            lang = "en"
        )

        assertThat(pdfBytes).isNotEmpty()
        assertThat(pdfBytes[0].toInt()).isEqualTo(0x25) // '%'
        assertThat(pdfBytes[1].toInt()).isEqualTo(0x50) // 'P'
        assertThat(pdfBytes[2].toInt()).isEqualTo(0x44) // 'D'
        assertThat(pdfBytes[3].toInt()).isEqualTo(0x46) // 'F'
    }
}
