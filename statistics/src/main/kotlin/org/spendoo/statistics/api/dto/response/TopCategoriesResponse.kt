package org.spendoo.statistics.api.dto.response

import com.fasterxml.jackson.annotation.JsonAlias
import java.math.BigDecimal

data class TopCategoriesResponse(
    @JsonAlias("total_spending")
    val totalSpending: BigDecimal,
    @JsonAlias("top_categories")
    val topCategories: List<CategorySpendingDto>
)
