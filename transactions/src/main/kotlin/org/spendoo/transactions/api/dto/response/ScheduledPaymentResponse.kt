package org.spendoo.transactions.api.dto.response

import org.spendoo.transactions.entity.ReminderUnit
import org.spendoo.transactions.entity.ScheduledPayment
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

data class ScheduledPaymentResponse(
    val id: UUID,
    val title: String,
    val amount: BigDecimal,
    val categoryId: UUID,
    val nextDueDate: LocalDateTime,
    val nextReminderDate: LocalDateTime,
    val frequency: Int,
    val reminderPeriod: Int,
    val reminderUnit: ReminderUnit
)

fun ScheduledPayment.toResponse(): ScheduledPaymentResponse {
    return ScheduledPaymentResponse(
        id = this.id,
        title = this.title,
        amount = this.amount,
        categoryId = this.categoryId,
        nextDueDate = this.nextDueDate,
        nextReminderDate = this.nextReminderDate,
        frequency = this.frequency,
        reminderPeriod = this.reminderPeriod,
        reminderUnit = this.reminderUnit
    )
}