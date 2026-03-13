package org.spendoo.categories.mapper

import org.spendoo.categories.api.dto.request.CategoryCreateRequest
import org.spendoo.categories.api.dto.response.BudgetResponse
import org.spendoo.categories.api.dto.response.CategoryResponse
import org.spendoo.categories.entity.Budget
import org.spendoo.categories.entity.Category
import org.spendoo.categories.entity.CategoryIcon
import org.spendoo.categories.entity.LeftOverOptions
import java.util.*

//@Component

fun CategoryCreateRequest.toEntity(userId: UUID): Category {
    return Category(
        userId = userId,
        categoryName = this.categoryName,
        categoryIcon = CategoryIcon.valueOf(this.categoryIcon.uppercase()),
        leftOverOptions = LeftOverOptions.valueOf(this.leftOverOptions.uppercase()),
        priority = this.priority,
    )
}

class CategoryMapper {


    fun toResponse(category: Category, budget: Budget? = null): CategoryResponse {
        return CategoryResponse(
//            categoryId = category.categoryId,
            categoryName = category.categoryName,
            categoryIcon = category.categoryIcon.name,
            priority = category.priority,
            leftOverOptions = category.leftOverOptions.name,
            budget = budget?.let { toBudgetResponse(it) }
        )
    }

    private fun toBudgetResponse(budget: Budget): BudgetResponse {

        val spentAmount = 0.0
        val spendingPercentage = if (budget.amount > 0) ((spentAmount / budget.amount) * 100).toInt() else 0

        return BudgetResponse(
            amount = budget.amount,
            spentAmount = spentAmount,
            spendingPercentage = spendingPercentage,
            startDate = budget.startDate,
            endDate = budget.endDate
        )
    }


}