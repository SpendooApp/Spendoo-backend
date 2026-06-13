package org.spendoo.statistics.api.controller

import org.spendoo.statistics.api.dto.response.StatisticsResponse
import org.spendoo.statistics.model.StatsPeriod
import org.spendoo.statistics.service.StatisticsService
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import java.util.*

import org.spendoo.statistics.model.ImageFormat
import org.spendoo.statistics.model.Language
import org.spendoo.statistics.model.Theme

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
        @RequestParam(defaultValue = "MONTHLY") period: StatsPeriod,
        @RequestHeader(name = "X-App-Theme", defaultValue = "LIGHT") theme: Theme,
        @RequestHeader(name = "Accept-Language", defaultValue = "EN") lang: Language
    ): ResponseEntity<ByteArray> {
        val pdfBytes = statisticsService.getStatisticsPdf(userId, period, theme, lang)
        val formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm")
        val timestamp = java.time.LocalDateTime.now().format(formatter)
        val filename = "statistics_report_${lang.name}_${theme.name}_$timestamp.pdf"
        
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"$filename\"")
            .contentType(MediaType.APPLICATION_PDF)
            .body(pdfBytes)
    }
}
