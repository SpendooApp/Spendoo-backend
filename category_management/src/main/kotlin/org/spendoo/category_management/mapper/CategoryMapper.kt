package org.spendoo.category_management.mapper

import org.spendoo.category_management.api.dto.request.CategoryCreateRequest
import org.spendoo.category_management.api.dto.response.BudgetResponse
import org.spendoo.category_management.api.dto.response.CategoryResponse
import org.spendoo.category_management.entity.Budget
import org.spendoo.category_management.entity.Category
import org.spendoo.category_management.entity.CategoryIcon
import org.spendoo.category_management.entity.LeftOverOptions
import java.util.UUID

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