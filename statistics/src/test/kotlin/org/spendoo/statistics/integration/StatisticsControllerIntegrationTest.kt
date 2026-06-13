package org.spendoo.statistics.integration

import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.spendoo.statistics.api.controller.StatisticsController
import org.spendoo.statistics.api.dto.response.*
import org.spendoo.statistics.model.StatsPeriod
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
        val mockResponse = StatisticsResponse(
            lineChart = LineChartData(emptyList(), emptyList(), emptyList(), emptyList(), BigDecimal.ZERO, 0, "mock"),
            barChart = BarChartData(emptyList(), emptyList(), "mock"),
            donutChart = DonutChartData(BigDecimal.ZERO, emptyList(), "mock"),
            topCategories = emptyList()
        )

        every {
            statisticsService.getStatistics(
                userId = any(),
                period = any(),
                theme = any(),
                lang = any(),
                imageFormat = any(),
                referenceDate = any()
            )
        } returns mockResponse

        val response = controller.getStatistics(userId, StatsPeriod.MONTHLY, org.spendoo.statistics.model.ImageFormat.BASE64, org.spendoo.statistics.model.Theme.LIGHT, org.spendoo.statistics.model.Language.EN)
        
        org.junit.jupiter.api.Assertions.assertEquals(HttpStatus.OK, response.statusCode)
        org.junit.jupiter.api.Assertions.assertEquals(mockResponse, response.body)
    }
}
