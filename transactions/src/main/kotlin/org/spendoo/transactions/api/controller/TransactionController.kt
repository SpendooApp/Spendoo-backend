package org.spendoo.transactions.api.controller

import jakarta.validation.Valid
import org.spendoo.transactions.api.dto.request.CreateTransactionRequest
import org.spendoo.transactions.service.TransactionService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/v1/transactions")
class TransactionController(
    private val transactionService: TransactionService
) {

    @PostMapping
    fun createTransactions(
        @Valid @RequestBody request: CreateTransactionRequest,
        @AuthenticationPrincipal userId: UUID ): ResponseEntity<Unit> {

        transactionService.createTransactions(userId, request)
        return ResponseEntity.status(HttpStatus.CREATED).build()
    }
}