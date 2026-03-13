package org.spendoo.categories.api.dto.request

import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import java.time.LocalDate

data class BudgetCreateRequest(

    @field:NotNull(message = "Amount is required")
    @field:Positive(message = "Amount must be greater than 0")
    val amount: Double,

    @field:NotNull(message = "Period is required")
    @field:Min(value = 1, message = "Period must be at least 1")
    val period: Int,

    @field:NotNull(message = "Start date is required")
    val startDate: LocalDate,

    )

