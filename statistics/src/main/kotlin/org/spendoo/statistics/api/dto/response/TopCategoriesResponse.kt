package org.spendoo.statistics.api.dto.response

import com.fasterxml.jackson.annotation.JsonAlias
import java.math.BigDecimal

data class TopCategoriesResponse(
    @field:JsonAlias("total_spending")
    val totalSpending: BigDecimal,
    @field:JsonAlias("top_categories")
    val topCategories: List<CategorySpendingDto>
)
