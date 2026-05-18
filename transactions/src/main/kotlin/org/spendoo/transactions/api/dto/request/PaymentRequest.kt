package org.spendoo.transactions.api.dto.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import org.spendoo.transactions.entity.PaymentFrequency
import org.spendoo.transactions.entity.ReminderUnit
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
    val frequency: PaymentFrequency,

    val reminderPeriod: Int?,

    val reminderUnit: ReminderUnit?,
)