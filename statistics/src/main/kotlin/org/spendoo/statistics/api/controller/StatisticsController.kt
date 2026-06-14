package org.spendoo.statistics.api.controller

import org.spendoo.statistics.api.dto.response.StatisticsResponse
import org.spendoo.statistics.model.*
import org.spendoo.statistics.service.StatisticsService
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import java.time.LocalDateTime
import java.util.*

@RestController
@RequestMapping("/api/v1/statistics")
class StatisticsController(
    private val statisticsService: StatisticsService
) {

    @GetMapping
    fun getStatistics(
        @AuthenticationPrincipal userId: UUID,
        @RequestParam(defaultValue = "MONTHLY") period: StatsPeriod,
        @RequestParam(defaultValue = "BASE64") imageFormat: ImageFormat,
        @RequestHeader(name = "X-App-Theme", defaultValue = "LIGHT") theme: Theme,
        @RequestHeader(name = "Accept-Language", defaultValue = "EN") lang: Language
    ): ResponseEntity<StatisticsResponse> {
        val stats = statisticsService.getStatistics(userId, period, theme, lang, imageFormat)
        return ResponseEntity.ok(stats)
    }

    @GetMapping("/pdf")
    fun getStatisticsPdf(
        @AuthenticationPrincipal userId: UUID,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) startDate: LocalDateTime,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) endDate: LocalDateTime,
        @RequestParam(defaultValue = "CHARTS") reportType: ReportType,
        @RequestParam(defaultValue = "FULL") dataType: DataType,
        @RequestHeader(name = "X-App-Theme", defaultValue = "LIGHT") theme: Theme,
        @RequestHeader(name = "Accept-Language", defaultValue = "EN") lang: Language
    ): ResponseEntity<ByteArray> {
        val pdfBytes = statisticsService.getStatisticsPdf(userId, startDate, endDate, reportType, dataType, theme, lang)
        val formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm")
        val timestamp = LocalDateTime.now().format(formatter)
        val startStr = startDate.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        val endStr = endDate.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        val filename = "statistics_report_${reportType.name}_${dataType.name}_${startStr}_to_${endStr}_$timestamp.pdf"
        
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"$filename\"")
            .contentType(MediaType.APPLICATION_PDF)
            .body(pdfBytes)
    }
}
