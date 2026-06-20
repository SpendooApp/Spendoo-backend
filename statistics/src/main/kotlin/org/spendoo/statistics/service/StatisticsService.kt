package org.spendoo.statistics.service

import org.spendoo.client.ApiClient
import org.spendoo.i18n.I18nService
import org.spendoo.statistics.api.dto.response.CombinedStatsResponse
import org.spendoo.statistics.model.ReportDataType
import org.spendoo.statistics.model.Granularity
import org.spendoo.statistics.model.Language
import org.spendoo.statistics.model.Theme
import org.spendoo.statistics.util.PdfGenerator
import org.spendoo.transactions.repository.BudgetRepository
import org.spendoo.transactions.repository.TransactionRepository
import org.spendoo.transactions.repository.TransactionViewRepository
import org.springframework.http.HttpMethod
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.*

@Service
class StatisticsService(
    private val transactionRepository: TransactionRepository,
    private val budgetRepository: BudgetRepository,
    private val transactionViewRepository: TransactionViewRepository,
    private val i18nService: I18nService,
    private val apiClient: ApiClient
) {

    @Transactional(readOnly = true)
    fun getStatistics(
        userId: UUID,
        granularity: Granularity,
        startDate: LocalDateTime,
        endDate: LocalDateTime
    ): CombinedStatsResponse {
        val requestBody = mapOf(
            "user_id" to userId.toString(),
            "granularity" to granularity.name,
            "start_date" to startDate.toString(),
            "end_date" to endDate.toString()
        )

        return apiClient.call(CombinedStatsResponse::class.java) {
            callAIService = true
            path = "/api/v1/statistics/combined"
            method = HttpMethod.POST
            body = requestBody
        } ?: throw IllegalStateException("Failed to call AI service statistics")
    }

    @Transactional(readOnly = true)
    fun getStatisticsPdf(
        userId: UUID,
        startDate: LocalDateTime,
        endDate: LocalDateTime,
        reportDataType: ReportDataType,
        theme: Theme,
        lang: Language
    ): ByteArray {
        return PdfGenerator.generateDetailedPdf(
            userId = userId,
            startDate = startDate,
            endDate = endDate,
            reportDataType = reportDataType,
            theme = theme,
            lang = lang,
            i18nService = i18nService,
            transactionRepository = transactionRepository,
            transactionViewRepository = transactionViewRepository,
            budgetRepository = budgetRepository
        )
    }
}
