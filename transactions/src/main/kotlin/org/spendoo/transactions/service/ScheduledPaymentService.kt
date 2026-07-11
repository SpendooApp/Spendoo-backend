package org.spendoo.transactions.service

import org.spendoo.transactions.api.dto.request.*
import org.spendoo.transactions.api.dto.response.ScheduledPaymentResponse
import org.spendoo.transactions.api.dto.response.ScheduledPaymentsDashboardResponse
import org.spendoo.transactions.api.dto.response.toResponse
import org.spendoo.transactions.entity.ScheduledPayment
import org.spendoo.transactions.repository.CategoryRepository
import org.spendoo.transactions.repository.ScheduledPaymentRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.Instant
import java.time.ZoneOffset
import java.util.*

@Service
class ScheduledPaymentService (

    private val paymentRepository: ScheduledPaymentRepository,
    private val transactionService: TransactionService,
    private val categoryRepository: CategoryRepository,

    ) {

    @Transactional
    fun createPayment(userId: UUID, request: PaymentRequest){

        val category = categoryRepository.findByIdAndUserIdAndIsDeletedFalse(request.categoryId, userId)
            ?: throw IllegalArgumentException("Category not found ")
        val entity = request.toEntity(userId, category)
        paymentRepository.save(entity)
    }

    @Transactional
    fun updatePayment(userId: UUID, paymentId: UUID, request: PaymentRequest){
        val payment = getPaymentEntity(paymentId, userId)

        val category = categoryRepository.findByIdAndUserIdAndIsDeletedFalse(request.categoryId, userId)
            ?: throw IllegalArgumentException("Category not found")

        val newNextDueDate = request.frequency.alignNextDueDate(request.startDate)
        val newReminderDate = newNextDueDate.minusReminder(request.reminderPeriod, request.reminderUnit)

        val updatedPayment = payment.copy(
            title = request.title,
            amount = request.amount,
            category = category,
            startDate = request.startDate,
            frequency = request.frequency,
            nextDueDate = newNextDueDate,
            nextReminderDate = newReminderDate,
            isNotified = false,
            reminderPeriod = request.reminderPeriod,
            reminderUnit = request.reminderUnit
        )

        paymentRepository.save(updatedPayment)

    }


    @Transactional
    fun deletePayment(userId: UUID, paymentId: UUID){
        val payment = getPaymentEntity(paymentId, userId)
        paymentRepository.delete(payment)
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun payScheduledItem(userId: UUID, paymentId: UUID){
        val currentPayment = getPaymentEntity(paymentId, userId)


        val expenseRequest = CreateExpenseTransactionRequest(
            entries = listOf(
                ExpenseTransactionEntryDto(
                    title = currentPayment.title,
                    amount = currentPayment.amount,
                    categoryId = currentPayment.category.id,
                    transactionDate = Instant.now(),
                    note = "Payment for ${currentPayment.title}"
                )
            )
        )
        transactionService.createExpenseTransactions(userId, expenseRequest)

        val newNextDueDate = currentPayment.nextDueDate.atZone(ZoneOffset.UTC).plusDays(currentPayment.frequency.toLong()).toInstant()
        val newReminderDate = newNextDueDate.minusReminder(currentPayment.reminderPeriod, currentPayment.reminderUnit)

        val nextCyclePayment = currentPayment.copy(
            startDate = currentPayment.nextDueDate,
            nextDueDate = newNextDueDate,
            nextReminderDate = newReminderDate,
            isNotified = false
        )

        paymentRepository.save(nextCyclePayment)
    }

    @Transactional
    fun skipPayment(userId: UUID, paymentId: UUID){
        val currentPayment = getPaymentEntity(paymentId, userId)

        val newNextDueDate = currentPayment.nextDueDate.atZone(ZoneOffset.UTC).plusDays(currentPayment.frequency.toLong()).toInstant()
        val newReminderDate = newNextDueDate.minusReminder(currentPayment.reminderPeriod, currentPayment.reminderUnit)

        val skippedPayment = currentPayment.copy(
            startDate = currentPayment.nextDueDate,
            nextDueDate = newNextDueDate,
            nextReminderDate = newReminderDate,
            isNotified = false
        )
        paymentRepository.save(skippedPayment)
    }

    @Transactional(readOnly = true)
    fun getPaymentById(userId: UUID, paymentId: UUID): ScheduledPaymentResponse {
        val payment = getPaymentEntity(paymentId, userId)
        return payment.toResponse()
    }

    @Transactional(readOnly = true)
    fun getAllPayments(userId: UUID, pageable: Pageable): Page<ScheduledPaymentResponse> {

        return paymentRepository.findAllByUserId(userId, pageable)
            .map { it.toResponse() }
    }

    @Transactional(readOnly = true)
    fun getDashboardSummary(userId: UUID): ScheduledPaymentsDashboardResponse {
        val totalAmount = paymentRepository.sumAmountByUserId(userId) ?: BigDecimal.ZERO
        val upcomingCount = paymentRepository.countUpcomingByUserId(userId, Instant.now())

        return ScheduledPaymentsDashboardResponse(
            totalScheduledAmount = totalAmount,
            upcomingCount = upcomingCount
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