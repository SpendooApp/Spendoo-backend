package org.spendoo.categories.mapper

import org.spendoo.categories.api.dto.response.CategoryDashboardResponse
import org.spendoo.categories.entity.Budget
import org.spendoo.categories.entity.Category
import java.util.*

class CategoryDashboardMapper(
    private val categoryMapper: CategoryMapper
) {
    fun toDashboardResponse(
        categories: List<Category>,
        budgets: Map<UUID, Budget>,
        totalIncome: Double
    ): CategoryDashboardResponse {

        val categoryResponses = categories.map { category ->
            val budget = budgets[category.categoryId]
                ?: throw IllegalArgumentException("Budget not found for category ${category.categoryId}")

            categoryMapper.toResponse(category, budget)
        }
        val categorizedAmount =
            budgets.values.sumOf { it.amount }

        val remainingAmount =
            totalIncome - categorizedAmount

        return CategoryDashboardResponse(
            totalIncome = totalIncome,
            categorizedAmount = categorizedAmount,
            remainingAmount = remainingAmount,
            categories = categoryResponses
        )
    }
}