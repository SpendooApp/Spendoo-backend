package org.spendoo.statistics.api.controller

import org.spendoo.client.ApiClient
import org.spendoo.statistics.api.dto.response.CombinedStatsResponse
import org.spendoo.statistics.model.Granularity
import org.spendoo.statistics.model.ReportDataType
import org.spendoo.statistics.model.Theme
import org.spendoo.statistics.service.StatisticsService
import org.spendoo.transactions.entity.PlanCode
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.*
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.*

@RestController
@RequestMapping("/api/v1/statistics")
class StatisticsController(
    private val statisticsService: StatisticsService,
    private val apiClient: ApiClient
) {

    @GetMapping
    fun getStatistics(
        @AuthenticationPrincipal userId: UUID,
        @RequestParam granularity: Granularity,
        @RequestParam("start_date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) startDate: Instant,
        @RequestParam("end_date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) endDate: Instant
    ): ResponseEntity<CombinedStatsResponse> {
        responseEntityBodyBuilder(userId, granularity)?.let { return it.build() }
        val stats = statisticsService.getStatistics(userId, granularity, startDate, endDate)
        return ResponseEntity.ok(stats)
    }

    @GetMapping("/pdf")
    fun getStatisticsPdf(
        @AuthenticationPrincipal userId: UUID,
        @RequestParam("start_date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) startDate: Instant,
        @RequestParam("end_date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) endDate: Instant,
        @RequestParam(defaultValue = "FULL") reportDataType: ReportDataType,
        @RequestHeader(name = "X-App-Theme", defaultValue = "LIGHT") theme: Theme,
        @RequestHeader(name = "Accept-Language", defaultValue = "en") lang: String
    ): ResponseEntity<ByteArray> {
        responseEntityBodyBuilder(userId, timeRangeToGranularity(startDate, endDate))?.let { return it.build() }
        val pdfBytes = statisticsService.getStatisticsPdf(userId, startDate, endDate, reportDataType, theme, lang)
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm").withZone(ZoneOffset.UTC)
        val timestamp = formatter.format(Instant.now())
        val startStr = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneOffset.UTC).format(startDate)
        val endStr = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneOffset.UTC).format(endDate)
        val filename = "statistics_report_${reportDataType.name}_${startStr}_to_${endStr}_$timestamp.pdf"

        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"$filename\"")
            .contentType(MediaType.APPLICATION_PDF)
            .body(pdfBytes)
    }

    @GetMapping("/user/{targetUserId}")
    fun getUserStatistics(
        @AuthenticationPrincipal userId: UUID,
        @PathVariable targetUserId: UUID,
        @RequestParam granularity: Granularity,
        @RequestParam("start_date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) startDate: Instant,
        @RequestParam("end_date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) endDate: Instant
    ): ResponseEntity<CombinedStatsResponse> {
        responseEntityBodyBuilder(targetUserId, granularity)?.let { return it.build() }
        val stats = statisticsService.getUserStatistics(userId, targetUserId, granularity, startDate, endDate)
        return ResponseEntity.ok(stats)
    }

    @GetMapping("/user/{targetUserId}/pdf")
    fun getUserStatisticsPdf(
        @AuthenticationPrincipal userId: UUID,
        @PathVariable targetUserId: UUID,
        @RequestParam("start_date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) startDate: Instant,
        @RequestParam("end_date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) endDate: Instant,
        @RequestParam(defaultValue = "FULL") reportDataType: ReportDataType,
        @RequestHeader(name = "X-App-Theme", defaultValue = "LIGHT") theme: Theme,
        @RequestHeader(name = "Accept-Language", defaultValue = "en") lang: String
    ): ResponseEntity<ByteArray> {
        responseEntityBodyBuilder(targetUserId, timeRangeToGranularity(startDate, endDate))?.let { return it.build() }
        val pdfBytes = statisticsService.getUserStatisticsPdf(
            userId,
            targetUserId,
            startDate,
            endDate,
            reportDataType,
            theme,
            lang
        )
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm").withZone(ZoneOffset.UTC)
        val timestamp = formatter.format(Instant.now())
        val startStr = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneOffset.UTC).format(startDate)
        val endStr = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneOffset.UTC).format(endDate)
        val filename = "statistics_report_${reportDataType.name}_${startStr}_to_${endStr}_$timestamp.pdf"

        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"$filename\"")
            .contentType(MediaType.APPLICATION_PDF)
            .body(pdfBytes)


    }

    private fun getCurrentPlanCode(userId: UUID): PlanCode {
        val response = apiClient.call(PlanCode::class.java) {
            path = "/api/v1/subscriptions/current"
            method = HttpMethod.GET
            addToken = true
            this.userId = userId
        }
        return response ?: PlanCode.FREE
    }

    private fun responseEntityBodyBuilder(
        userId: UUID,
        granularity: Granularity
    ): ResponseEntity.BodyBuilder? = when (getCurrentPlanCode(userId)) {
        PlanCode.FREE -> {
            when (granularity) {
                Granularity.MONTH,
                Granularity.YEAR -> ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED)

                else -> {
                    null
                }
            }
        }

        PlanCode.BASIC -> when (granularity) {
            Granularity.YEAR -> ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED)
            else -> {
                null
            }
        }

        PlanCode.PRO -> {
            null
        }
    }

    private fun timeRangeToGranularity(startDate: Instant, endDate: Instant): Granularity {
        val duration = Duration.between(startDate, endDate)
        return when {
            duration.toDays() <= 7 -> Granularity.DAY
            duration.toDays() <= 31 -> Granularity.WEEK
            duration.toDays() <= 365 -> Granularity.MONTH
            else -> Granularity.YEAR
        }
    }
}
