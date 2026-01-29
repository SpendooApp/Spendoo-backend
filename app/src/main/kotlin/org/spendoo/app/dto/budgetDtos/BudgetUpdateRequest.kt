package org.spendoo.app.dto.budgetDtos

data class BudgetUpdateRequest(
    val amount: Double?,
    val spent: Double?,
    val period: String?,
    val startDate: String?,
    val endDate: String?,
    val carryOver: Boolean?
)
