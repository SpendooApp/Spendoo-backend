package org.spendoo.transactions.api.dto.response

import org.spendoo.transactions.entity.Budget
import org.spendoo.transactions.entity.Category
import org.spendoo.transactions.entity.CategoryIcon
import org.spendoo.transactions.entity.LeftOverOptions
import org.spendoo.transactions.service.model.CategoryParams
import java.math.BigDecimal
import java.util.*

data class CategoryResponse(
    val categoryId: UUID,

    val categoryName: String,

    val categoryIcon: CategoryIcon,

    val priority: Int,

    val leftOverOptions: LeftOverOptions,

    val budget: BudgetResponse?
)

fun Category.toResponse(budget: Budget? = null, spentAmount: BigDecimal = BigDecimal.ZERO): CategoryResponse {
    return CategoryResponse(
        categoryId = this.id,
        categoryName = this.categoryName,
        categoryIcon = this.categoryIcon,
        priority = this.priority,
        leftOverOptions = this.leftOverOptions,
        budget = budget?.toBudgetResponse(spentAmount)
    )
}

fun CategoryParams.toResponse(spentAmount: BigDecimal = BigDecimal.ZERO): CategoryResponse {
    return CategoryResponse(
        categoryId = this.categoryId,
        categoryName = this.categoryName,
        categoryIcon = this.categoryIcon,
        priority = this.priority,
        leftOverOptions = this.leftOverOptions,
        budget = budgetId?.let { this.toBudgetResponse(spentAmount) }
    )
}