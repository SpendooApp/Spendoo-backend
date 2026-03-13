package org.spendoo.categories.api.dto.request

import jakarta.validation.constraints.NotBlank
import org.spendoo.categories.entity.CategoryIcon
import org.spendoo.categories.entity.LeftOverOptions

data class CategoryUpdateRequest(
    @field:NotBlank(message = "Category name must not be blank")
    val categoryName: String?,

    val categoryIcon: CategoryIcon?,

    val leftOverOptions: LeftOverOptions?,

    val priority: Int?,

    val budget: BudgetCreateRequest?
)
