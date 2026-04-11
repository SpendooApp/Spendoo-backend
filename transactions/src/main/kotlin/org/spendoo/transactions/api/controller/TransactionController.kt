package org.spendoo.transactions.api.controller

import jakarta.validation.Valid
import org.spendoo.transactions.api.dto.request.CreateExpenseTransactionRequest
import org.spendoo.transactions.api.dto.request.CreateIncomeTransactionRequest
import org.spendoo.transactions.api.dto.request.TransactionUpdateRequest
import org.spendoo.transactions.api.dto.response.BalanceSummary
import org.spendoo.transactions.api.dto.response.CategorySpendingDto
import org.spendoo.transactions.api.dto.response.TransactionResponse
import org.spendoo.transactions.api.dto.response.toResponse
import org.spendoo.transactions.service.TransactionService
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import java.time.LocalDateTime
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
        pageable: Pageable
    ): ResponseEntity<Page<TransactionResponse>> {
        val page = transactionService.getAll(userId, pageable)
        return ResponseEntity.ok(page.map { it.toResponse() })
    }

    @GetMapping("/range")
    fun getTransactionsByDateRange(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) startDate: LocalDateTime,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) endDate: LocalDateTime,
        @AuthenticationPrincipal userId: UUID,
        pageable: Pageable
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

    @GetMapping("/top-spending")
    fun getTopSpending(
        @AuthenticationPrincipal userId: UUID,
        @PageableDefault(size = 5)
        pageable: Pageable,
    ): ResponseEntity<Page<CategorySpendingDto>> {
        val topSpending = transactionService.getTopSpendingCategories(userId, pageable)
        return ResponseEntity.ok(topSpending)
    }
}