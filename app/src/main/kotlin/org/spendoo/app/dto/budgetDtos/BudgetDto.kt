package org.spendoo.app.dto.budgetDtos

data class BudgetDto(
    val id: Long,
    val categoryId: Long,
    val categoryName: String,
    val amount: Double,
    val spent: Double,
    val period: String,
    val startDate: String,
    val endDate: String,
    val carryOver: Boolean
)
