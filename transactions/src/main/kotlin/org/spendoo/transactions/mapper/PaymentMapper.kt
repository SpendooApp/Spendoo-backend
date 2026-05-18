package org.spendoo.transactions.mapper

import org.spendoo.transactions.api.dto.request.PaymentRequest
import org.spendoo.transactions.api.dto.response.ScheduledPaymentResponse
import org.spendoo.transactions.entity.ScheduledPayment
import org.spendoo.transactions.entity.PaymentFrequency
import java.time.LocalDateTime
import java.util.UUID

fun getNextDate(date: LocalDateTime, frequency: PaymentFrequency): LocalDateTime {
    return when (frequency) {
        PaymentFrequency.DAILY -> date.plusDays(1)
        PaymentFrequency.WEEKLY -> date.plusWeeks(1)
        PaymentFrequency.MONTHLY -> date.plusMonths(1)
        PaymentFrequency.YEARLY -> date.plusYears(1)
    }
}

fun alignNextDueDate(startDate: LocalDateTime, frequency: PaymentFrequency): LocalDateTime {
    val now = LocalDateTime.now()
    var nextDue = startDate

    while (nextDue.isBefore(now)) {
        nextDue = getNextDate(nextDue, frequency)
    }
    return nextDue
}

fun PaymentRequest.toEntity(userId: UUID): ScheduledPayment {
    val firstPaymentDate = alignNextDueDate(this.startDate, this.frequency)

    return ScheduledPayment(
        userId = userId,
        categoryId = this.categoryId,
        title = this.title,
        amount = this.amount,
        startDate = this.startDate,
        nextDueDate = firstPaymentDate,
        frequency = this.frequency,
        reminderPeriod = this.reminderPeriod,
        reminderUnit = this.reminderUnit
    )
}

fun ScheduledPayment.toResponse(): ScheduledPaymentResponse {
    return ScheduledPaymentResponse(
        id = this.id,
        title = this.title,
        amount = this.amount,
        categoryId = this.categoryId,
        nextDueDate = this.nextDueDate,
        frequency = this.frequency,
        reminderPeriod = this.reminderPeriod,
        reminderUnit = this.reminderUnit
    )
}