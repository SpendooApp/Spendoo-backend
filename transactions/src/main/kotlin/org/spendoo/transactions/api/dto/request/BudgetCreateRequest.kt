package org.spendoo.transactions.api.dto.request

import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Positive
import org.spendoo.transactions.entity.Budget
import org.spendoo.transactions.entity.Category
import java.math.BigDecimal
import java.time.LocalDateTime

data class BudgetCreateRequest(

    @field:Positive(message = "Amount must be greater than 0")
    val amount: Double,

    @field:Min(value = 1, message = "Period must be at least 1")
    val period: Int,

    val startDate: LocalDateTime
)

fun BudgetCreateRequest.toBudget(category: Category, carryOver: BigDecimal, isActive: Boolean = true): Budget {

    val endDate = this.startDate.plusDays(this.period.toLong())
    return Budget(
        category = category,
        amount = this.amount.toBigDecimal() + carryOver,
        carryOver = carryOver,
        startDate = this.startDate,
        endDate = endDate,
        period = this.period,
        isActive = isActive
    )
}