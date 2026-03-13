package org.spendoo.categories.api.dto.response

data class CategoryDashboardResponse(

    val totalIncome: Double,

    val categorizedAmount: Double,

    val remainingAmount: Double,

    val categories: List<CategoryResponse>
)
