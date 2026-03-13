package org.spendoo.categories.api.dto.request

import jakarta.validation.constraints.NotBlank
import org.spendoo.categories.entity.Category
import org.spendoo.categories.entity.CategoryIcon
import org.spendoo.categories.entity.LeftOverOptions
import java.util.*

data class CategoryCreateRequest(
    @field:NotBlank(message = "Category name must not be blank")
    val categoryName: String,

    val categoryIcon: CategoryIcon,

    val leftOverOptions: LeftOverOptions,

    val priority: Int,

    val budget: BudgetCreateRequest
)

fun CategoryCreateRequest.toEntity(userId: UUID): Category {
    return Category(
        userId = userId,
        categoryName = this.categoryName,
        categoryIcon = categoryIcon,
        leftOverOptions = leftOverOptions,
        priority = this.priority,
    )
}