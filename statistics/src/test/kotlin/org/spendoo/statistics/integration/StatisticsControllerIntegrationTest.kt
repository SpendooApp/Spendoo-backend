package org.spendoo.statistics.integration

import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.spendoo.statistics.api.controller.StatisticsController
import org.spendoo.statistics.api.dto.response.*
import org.spendoo.statistics.model.Granularity
import org.spendoo.statistics.service.StatisticsService
import org.springframework.http.HttpStatus
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.*

class StatisticsControllerIntegrationTest {

    @Test
    fun `controller calls service and returns response`() {
        val statisticsService = mockk<StatisticsService>()
        val controller = StatisticsController(statisticsService)
        
        val userId = UUID.randomUUID()
        val mockResponse = CombinedStatsResponse(
            financialStats = FinancialStatsResponse(emptyList(), 0, BigDecimal.ZERO, false),
            budgetStatus = BudgetStatusResponse(emptyList(), BigDecimal.ZERO),
            topCategories = TopCategoriesResponse(BigDecimal.ZERO, emptyList())
        )

        every {
            statisticsService.getStatistics(
                userId = any(),
                granularity = any(),
                startDate = any(),
                endDate = any()
            )
        } returns mockResponse

        val response = controller.getStatistics(
            userId = userId,
            granularity = Granularity.MONTH,
            startDate = LocalDateTime.now(),
            endDate = LocalDateTime.now().plusDays(1)
        )
        
        org.junit.jupiter.api.Assertions.assertEquals(HttpStatus.OK, response.statusCode)
        org.junit.jupiter.api.Assertions.assertEquals(mockResponse, response.body)
    }
}
