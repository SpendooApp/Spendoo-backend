package org.spendoo.transactions.api.dto.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import org.spendoo.transactions.entity.Category
import org.spendoo.transactions.entity.ReminderUnit
import org.spendoo.transactions.entity.ScheduledPayment
import java.math.BigDecimal
import java.time.Instant
import java.time.ZoneOffset
import java.util.*

data class PaymentRequest(

    @field:NotBlank
    val title: String,

    @field:NotNull @field:Positive
    val amount: BigDecimal,

    @field:NotNull
    val categoryId: UUID,

    @field:NotNull
    val startDate: Instant,

    @field:NotNull
    val frequency: Int,

    @field:NotNull
    val reminderPeriod: Int,

    @field:NotNull
    val reminderUnit: ReminderUnit,
)

fun Int.alignNextDueDate(startDate: Instant): Instant {
    val now = Instant.now()
    var nextDue = startDate
    while (nextDue.isBefore(now)) {
        nextDue = nextDue.atZone(ZoneOffset.UTC).plusDays(this.toLong()).toInstant()
    }
    return nextDue
}

fun Instant.minusReminder(period: Int, unit: ReminderUnit): Instant {
    return when (unit) {
        ReminderUnit.HOUR -> this.atZone(ZoneOffset.UTC).minusHours(period.toLong()).toInstant()
        ReminderUnit.DAY -> this.atZone(ZoneOffset.UTC).minusDays(period.toLong()).toInstant()
        ReminderUnit.WEEK -> this.atZone(ZoneOffset.UTC).minusWeeks(period.toLong()).toInstant()
        ReminderUnit.MONTH -> this.atZone(ZoneOffset.UTC).minusMonths(period.toLong()).toInstant()
    }
}

fun PaymentRequest.toEntity(userId: UUID, category: Category): ScheduledPayment {

    val firstPaymentDate = this.frequency.alignNextDueDate(this.startDate)
    val reminderDate = firstPaymentDate.minusReminder(this.reminderPeriod, this.reminderUnit)

    return ScheduledPayment(
        userId = userId,
        title = this.title,
        amount = this.amount,
        startDate = this.startDate,
        category = category,
        nextDueDate = firstPaymentDate,
        nextReminderDate = reminderDate,
        frequency = this.frequency,
        reminderPeriod = this.reminderPeriod,
        reminderUnit = this.reminderUnit
    )
}