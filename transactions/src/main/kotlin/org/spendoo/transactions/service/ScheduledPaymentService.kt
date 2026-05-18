package org.spendoo.transactions.service

import org.spendoo.transactions.api.dto.request.CreateExpenseTransactionRequest
import org.spendoo.transactions.api.dto.request.ExpenseTransactionEntryDto
import org.spendoo.transactions.api.dto.request.PaymentRequest
import org.spendoo.transactions.api.dto.response.ScheduledPaymentResponse
import org.spendoo.transactions.api.dto.response.ScheduledPaymentsDashboardResponse
import org.spendoo.transactions.entity.ScheduledPayment
import org.spendoo.transactions.mapper.alignNextDueDate
import org.spendoo.transactions.mapper.getNextDate
import org.spendoo.transactions.mapper.toEntity
import org.spendoo.transactions.mapper.toResponse
import org.spendoo.transactions.repository.ScheduledPaymentRepository
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

@Service
class ScheduledPaymentService (

    private val paymentRepository: ScheduledPaymentRepository,
    private val transactionService: TransactionService
) {

    @Transactional
    fun createPayment(userId: UUID, request: PaymentRequest){
        val entity = request.toEntity(userId)
        paymentRepository.save(entity)
    }

    @Transactional
    fun updatePayment(userId: UUID, paymentId: UUID, request: PaymentRequest){
        val payment = getPaymentEntity(paymentId, userId)

        val updatedPayment= if(payment.startDate != request.startDate){

            payment.copy(
                title = request.title,
                amount = request.amount,
                categoryId = request.categoryId,
                frequency = request.frequency,
                startDate = request.startDate,
                nextDueDate = alignNextDueDate(request.startDate, request.frequency),
                reminderPeriod = request.reminderPeriod,
                reminderUnit = request.reminderUnit
            )
        }
        else {
            payment.copy(
                title = request.title,
                amount = request.amount,
                categoryId = request.categoryId,
                frequency = request.frequency,
                reminderPeriod = request.reminderPeriod,
                reminderUnit = request.reminderUnit
            )
        }

        paymentRepository.save(updatedPayment)

    }


    @Transactional
    fun deletePayment(userId: UUID, paymentId: UUID){
        val payment = getPaymentEntity(paymentId, userId)
        paymentRepository.delete(payment)
    }

    @Transactional
    fun payScheduledItem(userId: UUID, paymentId: UUID){
        val currentPayment = getPaymentEntity(paymentId, userId)


        val expenseRequest = CreateExpenseTransactionRequest(
            entries = listOf(
                ExpenseTransactionEntryDto(
                    title = currentPayment.title,
                    amount = currentPayment.amount,
                    categoryId = currentPayment.categoryId,
                    transactionDate = LocalDateTime.now(),
                    note = "Payment for ${currentPayment.title}"
                )
            )
        )
        transactionService.createExpenseTransactions(userId, expenseRequest)

        val nextCyclePayment = currentPayment.copy(
            startDate = currentPayment.nextDueDate,
            nextDueDate = getNextDate(currentPayment.nextDueDate, currentPayment.frequency)
        )

        paymentRepository.save(nextCyclePayment)
    }

    @Transactional
    fun skipPayment(userId: UUID, paymentId: UUID){
        val currentPayment = getPaymentEntity(paymentId, userId)

        val skippedPayment = currentPayment.copy(
            startDate = currentPayment.nextDueDate,
            nextDueDate = getNextDate(currentPayment.nextDueDate, currentPayment.frequency)
        )
        paymentRepository.save(skippedPayment)
    }

    @Transactional(readOnly = true)
    fun getPaymentById(userId: UUID, paymentId: UUID): ScheduledPaymentResponse {
        val payment = getPaymentEntity(paymentId, userId)
        return payment.toResponse()
    }

    @Transactional(readOnly = true)
    fun getAllPayments(userId: UUID, pageable: Pageable): ScheduledPaymentsDashboardResponse {

        val pageResult = paymentRepository.findAllByUserId(userId, pageable)
        val totalAmount = paymentRepository.sumAmountByUserId(userId) ?: BigDecimal.ZERO
        val totalCount = pageResult.totalElements

        return ScheduledPaymentsDashboardResponse(
            totalScheduledAmount = totalAmount,
            upcomingCount = totalCount,
            payments = pageResult.map{ it.toResponse() }
        )
    }


    private fun getPaymentEntity(paymentId: UUID, userId: UUID): ScheduledPayment{
        val payment = paymentRepository.findById(paymentId)
            .orElseThrow { RuntimeException("Payment not found with id: $paymentId") }

        if(payment.userId != userId) {
            throw RuntimeException("Unauthorized: This payment does not belong to the user")

        }
        return payment

    }

}