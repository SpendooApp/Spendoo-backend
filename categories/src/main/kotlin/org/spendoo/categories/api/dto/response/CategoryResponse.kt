package org.spendoo.categories.api.dto.response

data class CategoryResponse(

    val categoryName: String,

    val categoryIcon: String,

    val priority: Int,

    val leftOverOptions: String,

    val budget: BudgetResponse?
)
