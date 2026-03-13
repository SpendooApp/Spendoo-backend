package org.spendoo.categories.api.dto.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

data class CategoryCreateRequest(
    @field:NotBlank(message = "Category name must not be blank")
    val categoryName: String,

    @field:NotNull(message = "Category icon is required")
    val categoryIcon: String,

    @field:NotNull(message = "Left over option is required")
    val leftOverOptions: String,

    @field:NotNull(message = "Priority is required")
    val priority: Int,

    val budget: BudgetCreateRequest
)
