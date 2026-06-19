package org.spendoo.transactions.api.controller

import jakarta.validation.Valid
import org.spendoo.transactions.api.dto.request.PaymentRequest
import org.spendoo.transactions.api.dto.response.ScheduledPaymentResponse
import org.spendoo.transactions.api.dto.response.ScheduledPaymentsDashboardResponse
import org.spendoo.transactions.service.ScheduledPaymentService
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID


@RestController
@RequestMapping("/api/v1/scheduled-payments")
class ScheduledPaymentController(
    private val scheduledPaymentService: ScheduledPaymentService
) {

    @PostMapping
    fun createPayment(
        @Valid @RequestBody request: PaymentRequest,
        @AuthenticationPrincipal userId: UUID): ResponseEntity<Unit> {
        scheduledPaymentService.createPayment(userId, request)
        return ResponseEntity.status(HttpStatus.CREATED).build()
    }

    @GetMapping("/{paymentId}")
    fun getPaymentById(
        @PathVariable paymentId: UUID,
        @AuthenticationPrincipal userId: UUID
    ): ResponseEntity<ScheduledPaymentResponse> {
        val payment = scheduledPaymentService.getPaymentById(userId, paymentId)
        return ResponseEntity.ok(payment)
    }

    @GetMapping
    fun getAllPayments(
        @AuthenticationPrincipal userId: UUID,
        pageable: Pageable
    ): ResponseEntity<Page<ScheduledPaymentResponse>> {
        return ResponseEntity.ok(scheduledPaymentService.getAllPayments(userId, pageable))
    }

    @GetMapping("/dashboard")
    fun getDashboardSummary(
        @AuthenticationPrincipal userId: UUID
    ): ResponseEntity<ScheduledPaymentsDashboardResponse> {
        return ResponseEntity.ok(scheduledPaymentService.getDashboardSummary(userId))
    }

    @PatchMapping("/{paymentId}")
    fun updatePayment(
        @PathVariable paymentId: UUID,
        @Valid @RequestBody request: PaymentRequest,
        @AuthenticationPrincipal userId: UUID): ResponseEntity<Unit> {

        scheduledPaymentService.updatePayment(userId, paymentId, request)
        return ResponseEntity.ok().build()
    }

    @DeleteMapping("/{paymentId}")
    fun deletePayment(
        @PathVariable paymentId: UUID,
        @AuthenticationPrincipal userId: UUID): ResponseEntity<Unit> {
        scheduledPaymentService.deletePayment(userId, paymentId)
        return ResponseEntity.ok().build()
    }

    @PostMapping("/{paymentId}/pay")
    fun payScheduledItem(
        @PathVariable paymentId: UUID,
        @AuthenticationPrincipal userId: UUID): ResponseEntity<Unit>{
        scheduledPaymentService.payScheduledItem(userId, paymentId)
        return ResponseEntity.ok().build()
    }

    @PostMapping("/{paymentId}/skip")
    fun skipPayment(
        @PathVariable paymentId: UUID,
        @AuthenticationPrincipal userId: UUID): ResponseEntity<Unit>{
        scheduledPaymentService.skipPayment(userId, paymentId)
        return ResponseEntity.ok().build()
    }


}