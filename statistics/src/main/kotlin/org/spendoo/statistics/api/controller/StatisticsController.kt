package org.spendoo.statistics.api.controller

import org.spendoo.statistics.api.dto.response.CombinedStatsResponse
import org.spendoo.statistics.model.Granularity
import org.spendoo.statistics.model.Language
import org.spendoo.statistics.model.ReportDataType
import org.spendoo.statistics.model.Theme
import org.spendoo.statistics.service.StatisticsService
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

@RestController
@RequestMapping("/api/v1/statistics")
class StatisticsController(
    private val statisticsService: StatisticsService
) {

    @GetMapping
    fun getStatistics(
        @AuthenticationPrincipal userId: UUID,
        @RequestParam granularity: Granularity,
        @RequestParam("start_date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) startDate: LocalDateTime,
        @RequestParam("end_date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) endDate: LocalDateTime
    ): ResponseEntity<CombinedStatsResponse> {
        val stats = statisticsService.getStatistics(userId, granularity, startDate, endDate)
        return ResponseEntity.ok(stats)
    }

    @GetMapping("/pdf")
    fun getStatisticsPdf(
        @AuthenticationPrincipal userId: UUID,
        @RequestParam("start_date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) startDate: LocalDateTime,
        @RequestParam("end_date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) endDate: LocalDateTime,
        @RequestParam(defaultValue = "FULL") reportDataType: ReportDataType,
        @RequestHeader(name = "X-App-Theme", defaultValue = "LIGHT") theme: Theme,
        @RequestHeader(name = "Accept-Language", defaultValue = "EN") lang: Language
    ): ResponseEntity<ByteArray> {
        val pdfBytes = statisticsService.getStatisticsPdf(userId, startDate, endDate, reportDataType, theme, lang)
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm")
        val timestamp = LocalDateTime.now().format(formatter)
        val startStr = startDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        val endStr = endDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        val filename = "statistics_report_${reportDataType.name}_${startStr}_to_${endStr}_$timestamp.pdf"
        
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"$filename\"")
            .contentType(MediaType.APPLICATION_PDF)
            .body(pdfBytes)
    }
}
