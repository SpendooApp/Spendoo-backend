package org.spendoo.app.dto.budgetDtos

data class BudgetCreateRequest(
    val categoryId: Long,
    val amount: Double,
    val period: String,
    val startDate: String,
    val endDate: String,
    val carryOver: Boolean
)
