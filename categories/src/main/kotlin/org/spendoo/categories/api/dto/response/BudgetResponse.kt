package org.spendoo.categories.api.dto.response

import org.spendoo.categories.entity.Budget
import org.spendoo.categories.service.model.CategoryParams
import java.math.BigDecimal
import java.time.LocalDateTime

data class BudgetResponse(

    val amount: BigDecimal,

    val spentAmount: BigDecimal,

    val spendingPercentage: Int,

    val period: Int,

    val startDate: LocalDateTime,

    val endDate: LocalDateTime
)

fun Budget.toBudgetResponse(): BudgetResponse {

    val spentAmount = BigDecimal.ZERO
    val spendingPercentage =
        if (this.amount > BigDecimal.ZERO) ((spentAmount / this.amount) * BigDecimal(100)).toInt() else 0

    return BudgetResponse(
        amount = this.amount,
        spentAmount = spentAmount,
        spendingPercentage = spendingPercentage,
        period = this.period,
        startDate = this.startDate,
        endDate = this.endDate
    )
}

fun CategoryParams.toBudgetResponse(): BudgetResponse {
    val amount = this.amount ?: BigDecimal.ZERO

    val spentAmount = BigDecimal.ZERO
    val spendingPercentage =
        if (amount > BigDecimal.ZERO) ((spentAmount / amount) * BigDecimal(100)).toInt() else 0
    return BudgetResponse(
        amount = amount,
        spentAmount = spentAmount,
        spendingPercentage = spendingPercentage,
        period = period ?: 0,
        startDate = startDate ?: LocalDateTime.now(),
        endDate = endDate ?: LocalDateTime.now()
    )
}
