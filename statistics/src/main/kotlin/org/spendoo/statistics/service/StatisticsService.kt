package org.spendoo.statistics.service

import org.spendoo.client.ApiClient
import org.spendoo.i18n.I18nService
import org.spendoo.statistics.api.dto.response.CombinedStatsResponse
import org.spendoo.statistics.model.Granularity
import org.spendoo.statistics.model.Language
import org.spendoo.statistics.model.ReportDataType
import org.spendoo.statistics.model.Theme
import org.spendoo.statistics.util.PdfGenerator
import org.spendoo.transactions.repository.TransactionViewRepository
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.*

@Service
class StatisticsService(
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
        val goalsSummary = apiClient.call(Map::class.java) {
            path = "/api/v1/goals/goals-summary"
            method = HttpMethod.GET
            addToken = true
            this.userId = userId
        }
        val totalSaved = when (val totalSavedVal = goalsSummary?.get("totalSaved")) {
            is Number -> BigDecimal(totalSavedVal.toDouble())
            is String -> BigDecimal(totalSavedVal)
            else -> BigDecimal.ZERO
        }

        return PdfGenerator.generateDetailedPdf(
            userId = userId,
            startDate = startDate,
            endDate = endDate,
            reportDataType = reportDataType,
            theme = theme,
            lang = lang,
            i18nService = i18nService,
            transactionViewRepository = transactionViewRepository,
            totalSaved = totalSaved
        )
    }

    @Transactional(readOnly = true)
    fun getUserStatistics(
        currentUserId: UUID,
        targetUserId: UUID,
        granularity: Granularity,
        startDate: LocalDateTime,
        endDate: LocalDateTime
    ): CombinedStatsResponse {
        validateFollowPermission(currentUserId, targetUserId)

        return getStatistics(targetUserId, granularity, startDate, endDate)
    }

    @Transactional(readOnly = true)
    fun getUserStatisticsPdf(
        currentUserId: UUID,
        targetUserId: UUID,
        startDate: LocalDateTime,
        endDate: LocalDateTime,
        reportDataType: ReportDataType,
        theme: Theme,
        lang: Language
    ): ByteArray {

        validateFollowPermission(currentUserId, targetUserId)

        return getStatisticsPdf(targetUserId, startDate, endDate, reportDataType, theme, lang)
    }

    private fun validateFollowPermission(currentUserId: UUID, targetUserId: UUID) {
        if (currentUserId == targetUserId) return

        val response = apiClient.call(Map::class.java) {
            path = "/api/v1/identity/follows/check-status?followerId=$currentUserId&followeeId=$targetUserId"
            method = HttpMethod.GET
            addToken = true
        }

        val isFollowing = response?.get("isFollowing") as? Boolean ?: false

        if (!isFollowing) {
            throw ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "You must follow this user to view their statistics"
            )
        }
    }

}
