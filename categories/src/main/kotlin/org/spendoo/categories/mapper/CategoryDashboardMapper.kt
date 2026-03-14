package org.spendoo.categories.mapper

import org.spendoo.categories.api.dto.response.CategoriesOverViewResponse
import org.spendoo.categories.entity.Budget
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
