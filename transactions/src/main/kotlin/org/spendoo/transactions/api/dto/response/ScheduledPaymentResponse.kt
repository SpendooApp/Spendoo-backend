package org.spendoo.transactions.api.dto.response

import org.spendoo.transactions.entity.PaymentFrequency
import org.spendoo.transactions.entity.ReminderUnit
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

data class ScheduledPaymentResponse(
    val id: UUID,
    val title: String,
    val amount: BigDecimal,
    val categoryId: UUID,
    val nextDueDate: LocalDateTime,
    val frequency: PaymentFrequency,
    val reminderPeriod: Int?,
    val reminderUnit: ReminderUnit?
)