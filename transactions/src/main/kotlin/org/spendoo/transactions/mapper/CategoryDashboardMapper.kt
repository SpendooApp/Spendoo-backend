package org.spendoo.transactions.mapper

import org.spendoo.transactions.api.dto.response.CategoriesOverViewResponse
import org.spendoo.transactions.entity.Budget
import java.math.BigDecimal
import java.util.*

fun toDashboardResponse(
    budgets: Map<UUID, Budget>,
    totalIncome: BigDecimal
): CategoriesOverViewResponse {

    val categorizedAmount =
        budgets.values.sumOf { it.amount }

    val remainingAmount =
        totalIncome - categorizedAmount

    return CategoriesOverViewResponse(
        totalIncome = totalIncome,
        categorizedAmount = categorizedAmount,
        remainingAmount = remainingAmount,
    )
}
