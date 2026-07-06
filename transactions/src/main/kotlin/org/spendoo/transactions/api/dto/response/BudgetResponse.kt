package org.spendoo.transactions.api.dto.response

import org.spendoo.transactions.entity.Budget
import org.spendoo.transactions.service.model.CategoryParams
import java.math.BigDecimal
import java.time.Instant

data class BudgetResponse(

    val amount: BigDecimal,

    val spentAmount: BigDecimal,

    val spendingPercentage: Int,

    val period: Int,

    val startDate: Instant,

    val endDate: Instant
)

fun Budget.toBudgetResponse(spentAmount: BigDecimal = BigDecimal.ZERO): BudgetResponse {

    val absoluteSpent = spentAmount.abs()
    val spendingPercentage =
        if (this.amount > BigDecimal.ZERO) ((absoluteSpent / this.amount) * BigDecimal(100)).toInt() else 0

    return BudgetResponse(
        amount = this.amount,
        spentAmount = spentAmount,
        spendingPercentage = spendingPercentage,
        period = this.period,
        startDate = this.startDate,
        endDate = this.endDate
    )
}

fun CategoryParams.toBudgetResponse(spentAmount: BigDecimal?): BudgetResponse {

    val amount = this.amount ?: BigDecimal.ZERO
    val spentAmount = spentAmount ?: BigDecimal.ZERO
    val absoluteSpent = spentAmount?.abs() ?: BigDecimal.ZERO
    val spendingPercentage =
        if (amount > BigDecimal.ZERO) ((absoluteSpent / amount) * BigDecimal(100)).toInt() else 0
    return BudgetResponse(
        amount = amount,
        spentAmount = spentAmount,
        spendingPercentage = spendingPercentage,
        period = period ?: 0,
        startDate = startDate ?: Instant.now(),
        endDate = endDate ?: Instant.now()
    )
}
