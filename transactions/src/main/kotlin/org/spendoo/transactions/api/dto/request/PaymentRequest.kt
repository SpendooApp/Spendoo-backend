package org.spendoo.transactions.api.dto.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import org.spendoo.transactions.entity.ReminderUnit
import org.spendoo.transactions.entity.ScheduledPayment
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

data class PaymentRequest(

    @field:NotBlank
    val title: String,

    @field:NotNull @field:Positive
    val amount: BigDecimal,

    @field:NotNull
    val categoryId: UUID,

    @field:NotNull
    val startDate: LocalDateTime,

    @field:NotNull
    val frequency: Int,

    @field:NotNull
    val reminderPeriod: Int,

    @field:NotNull
    val reminderUnit: ReminderUnit,
)

fun Int.alignNextDueDate(startDate: LocalDateTime): LocalDateTime {
    val now = LocalDateTime.now()
    var nextDue = startDate

    while (nextDue.isBefore(now)) {
        nextDue = nextDue.plusDays(this.toLong())
    }
    return nextDue
}

fun LocalDateTime.minusReminder(period: Int, unit: ReminderUnit): LocalDateTime {
    return when (unit) {
        ReminderUnit.HOUR -> this.minusHours(period.toLong())
        ReminderUnit.DAY -> this.minusDays(period.toLong())
        ReminderUnit.WEEK -> this.minusWeeks(period.toLong())
        ReminderUnit.MONTH -> this.minusMonths(period.toLong())
    }
}

fun PaymentRequest.toEntity(userId: UUID): ScheduledPayment {

    val firstPaymentDate = this.frequency.alignNextDueDate(this.startDate)
    val reminderDate = firstPaymentDate.minusReminder(this.reminderPeriod, this.reminderUnit)

    return ScheduledPayment(
        userId = userId,
        title = this.title,
        amount = this.amount,
        categoryId = this.categoryId,
        startDate = this.startDate,
        nextDueDate = firstPaymentDate,
        nextReminderDate = reminderDate,
        frequency = this.frequency,
        reminderPeriod = this.reminderPeriod,
        reminderUnit = this.reminderUnit
    )
}