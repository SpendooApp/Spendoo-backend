package org.spendoo.transactions.api.dto.request

import jakarta.validation.constraints.Pattern
import org.spendoo.transactions.entity.Category
import org.spendoo.transactions.entity.CategoryIcon
import org.spendoo.transactions.entity.LeftOverOptions
import java.util.*

data class CategoryUpdateRequest(
    @field:Pattern(regexp = ".*\\S.*", message = "Category name must not be blank")
    val categoryName: String,

    val categoryIcon: CategoryIcon,

    val leftOverOptions: LeftOverOptions,

    val priority: Int,

    val budget: BudgetCreateRequest?,
)

fun CategoryUpdateRequest.toEntity(categoryId: UUID, userId: UUID): Category {
    return Category(
        id = categoryId,
        userId = userId,
        categoryName = this.categoryName,
        categoryIcon = categoryIcon,
        leftOverOptions = leftOverOptions,
        priority = this.priority
    )
}