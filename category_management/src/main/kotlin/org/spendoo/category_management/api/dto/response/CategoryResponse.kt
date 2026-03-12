package org.spendoo.category_management.api.dto.response

data class CategoryResponse(

    val categoryName: String,

    val categoryIcon: String,

    val priority: Int,

    val leftOverOptions: String,

    val budget : BudgetResponse?
    )
