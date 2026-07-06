package org.spendoo.statistics.integration

import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.spendoo.client.ApiClient
import org.spendoo.statistics.api.controller.StatisticsController
import org.spendoo.statistics.api.dto.response.BudgetStatusResponse
import org.spendoo.statistics.api.dto.response.CombinedStatsResponse
import org.spendoo.statistics.api.dto.response.FinancialStatsResponse
import org.spendoo.statistics.api.dto.response.TopCategoriesResponse
import org.spendoo.statistics.model.Granularity
import org.spendoo.statistics.service.StatisticsService
import org.spendoo.transactions.entity.PlanCode
import org.springframework.http.HttpStatus
import java.math.BigDecimal
import java.time.Instant
import java.time.ZoneOffset
import java.util.*

class StatisticsControllerIntegrationTest {

    @Test
    fun `controller calls service and returns response`() {
        val statisticsService = mockk<StatisticsService>()
        val apiClient = mockk<ApiClient>()
        val controller = StatisticsController(statisticsService, apiClient)
        
        val userId = UUID.randomUUID()
        every { apiClient.call<PlanCode>(PlanCode::class.java, any()) } returns PlanCode.PRO
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
            startDate = Instant.now(),
            endDate = Instant.now().atZone(ZoneOffset.UTC).plusDays((1).toLong()).toInstant()
        )
        
        org.junit.jupiter.api.Assertions.assertEquals(HttpStatus.OK, response.statusCode)
        org.junit.jupiter.api.Assertions.assertEquals(mockResponse, response.body)
    }
}
