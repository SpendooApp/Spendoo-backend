package org.spendoo.category_management.mapper

import org.spendoo.category_management.api.dto.request.CategoryCreateRequest
import org.spendoo.category_management.api.dto.response.BudgetResponse
import org.spendoo.category_management.api.dto.response.CategoryResponse
import org.spendoo.category_management.entity.Budget
import org.spendoo.category_management.entity.Category
import org.spendoo.category_management.entity.CategoryIcon
import org.spendoo.category_management.entity.LeftOverOptions
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class CategoryMapper {

    fun toEntity(request: CategoryCreateRequest, userId: UUID): Category {
        return Category(
            userId = userId,
            categoryName = request.categoryName,
            categoryIcon = CategoryIcon.valueOf(request.categoryIcon.uppercase()),
            leftOverOptions = LeftOverOptions.valueOf(request.leftOverOptions.uppercase()),
            priority = request.priority,
        )
    }

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