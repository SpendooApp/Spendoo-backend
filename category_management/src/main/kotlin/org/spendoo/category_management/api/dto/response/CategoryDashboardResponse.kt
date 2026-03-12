package org.spendoo.category_management.api.dto.response

data class CategoryDashboardResponse(

    val totalIncome: Double,

    val categorizedAmount: Double,

    val remainingAmount: Double,

    val categories: List<CategoryResponse>
)
