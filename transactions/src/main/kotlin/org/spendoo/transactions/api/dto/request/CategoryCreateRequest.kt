package org.spendoo.transactions.api.dto.request

import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import org.spendoo.transactions.entity.Category
import org.spendoo.transactions.entity.CategoryIcon
import org.spendoo.transactions.entity.LeftOverOptions
import java.util.*

data class CategoryCreateRequest(
    @field:NotBlank(message = "Category name must not be blank")
    val categoryName: String,

    val categoryIcon: CategoryIcon,

    val leftOverOptions: LeftOverOptions,

    val priority: Int,

    @field:Valid
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