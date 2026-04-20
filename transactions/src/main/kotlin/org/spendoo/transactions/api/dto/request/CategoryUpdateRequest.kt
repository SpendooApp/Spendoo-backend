package org.spendoo.transactions.api.dto.request

import jakarta.validation.Valid
import jakarta.validation.constraints.Pattern
import org.spendoo.transactions.entity.CategoryIcon
import org.spendoo.transactions.entity.LeftOverOptions

data class CategoryUpdateRequest(
    @field:Pattern(regexp = ".*\\S.*", message = "Category name must not be blank")
    val categoryName: String,

    val categoryIcon: CategoryIcon,

    val leftOverOptions: LeftOverOptions,

    val priority: Int,

    @field:Valid
    val budget: BudgetCreateRequest,
)