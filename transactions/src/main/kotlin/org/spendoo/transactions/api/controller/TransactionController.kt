package org.spendoo.transactions.api.controller

import jakarta.validation.Valid
import org.spendoo.transactions.api.dto.request.CreateExpenseTransactionRequest
import org.spendoo.transactions.api.dto.request.CreateIncomeTransactionRequest
import org.spendoo.transactions.api.dto.request.TransactionUpdateRequest
import org.spendoo.transactions.api.dto.response.BalanceSummary
import org.spendoo.transactions.api.dto.response.EnrichedAiExtractionResponse
import org.spendoo.transactions.api.dto.response.FrequencyItemsResponse
import org.spendoo.transactions.api.dto.response.TransactionResponse
import org.spendoo.transactions.api.dto.response.toResponse
import org.spendoo.transactions.service.TransactionService
import org.springdoc.core.annotations.ParameterObject
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.time.Instant
import java.util.*

@RestController
@RequestMapping("/api/v1/transactions")
class TransactionController(
    private val transactionService: TransactionService
) {

    @PostMapping("/expense")
    fun createExpenseTransactions(
        @Valid @RequestBody request: CreateExpenseTransactionRequest,
        @AuthenticationPrincipal userId: UUID
    ): ResponseEntity<Unit> {
        transactionService.createExpenseTransactions(userId, request)
        return ResponseEntity.status(HttpStatus.CREATED).build()
    }

    @PostMapping("/income")
    fun createIncomeTransactions(
        @Valid @RequestBody request: CreateIncomeTransactionRequest,
        @AuthenticationPrincipal userId: UUID
    ): ResponseEntity<Unit> {
        transactionService.createIncomeTransactions(userId, request)
        return ResponseEntity.status(HttpStatus.CREATED).build()
    }

    @PatchMapping("/{transactionId}")
    fun updateTransaction(
        @PathVariable transactionId: UUID,
        @Valid @RequestBody updateRequest: TransactionUpdateRequest,
        @AuthenticationPrincipal userId: UUID
    ): ResponseEntity<Unit> {
        transactionService.updateTransaction(transactionId, userId, updateRequest)
        return ResponseEntity.ok().build()
    }

    @GetMapping("/{transactionId}")
    fun getTransactionById(
        @PathVariable transactionId: UUID,
        @AuthenticationPrincipal userId: UUID
    ): ResponseEntity<TransactionResponse> {
        val transaction = transactionService.getTransactionById(transactionId, userId)
        return ResponseEntity.ok(transaction.toResponse())
    }

    @GetMapping
    fun getAllTransactions(
        @AuthenticationPrincipal userId: UUID,
        @RequestParam(required = false) search: String?,
        @ParameterObject pageable: Pageable
    ): ResponseEntity<Page<TransactionResponse>> {
        val page = transactionService.getAll(userId, search, pageable)
        return ResponseEntity.ok(page.map { it.toResponse() })
    }

    @GetMapping("/range")
    fun getTransactionsByDateRange(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) startDate: Instant,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) endDate: Instant,
        @AuthenticationPrincipal userId: UUID,
        @ParameterObject pageable: Pageable
    ): ResponseEntity<Page<TransactionResponse>> {
        val page = transactionService.getTransactionsByDateRange(userId, startDate, endDate, pageable)
        return ResponseEntity.ok(page.map { it.toResponse() })
    }

    @DeleteMapping("/{transactionId}")
    fun deleteTransaction(
        @PathVariable transactionId: UUID,
        @AuthenticationPrincipal userId: UUID
    ): ResponseEntity<Unit> {
        transactionService.deleteTransaction(userId, transactionId)
        return ResponseEntity.ok().build()
    }


    @GetMapping("/summary")
    fun getTransactionSummary(@AuthenticationPrincipal userId: UUID): ResponseEntity<BalanceSummary> {
        val summary = transactionService.getBalanceSummary(userId)
        return ResponseEntity.ok(summary)
    }

    @GetMapping("/top-frequency-items")
    fun getTopFrequencyItems(
        @AuthenticationPrincipal userId: UUID,
        categoryId: UUID? = null,
        @PageableDefault(size = 10) pageable: Pageable
    ): ResponseEntity<Page<FrequencyItemsResponse>> {
        val page = transactionService.getTopFrequencyItems(userId, categoryId , pageable)
        return ResponseEntity.ok(page)
    }

    @PostMapping("/voice/process", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun processVoiceTransaction(
        @RequestParam("file") file: MultipartFile,
        @AuthenticationPrincipal userId: UUID
    ): ResponseEntity<EnrichedAiExtractionResponse> {
        val enrichedResponse = transactionService.processVoiceTransaction(file, userId)
        return if (enrichedResponse != null) {
            ResponseEntity.ok(enrichedResponse)
        } else {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
        }
    }

    @PostMapping("/ocr/scan", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun processOcrTransaction(
        @RequestParam("file") file: MultipartFile,
        @AuthenticationPrincipal userId: UUID
    ): ResponseEntity<EnrichedAiExtractionResponse> {
        val enrichedResponse = transactionService.processOcrTransaction(file, userId)
        return if (enrichedResponse != null) {
            ResponseEntity.ok(enrichedResponse)
        } else {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
        }
    }
}