package org.spendoo.statistics.api.dto.response

import com.fasterxml.jackson.annotation.JsonAlias
import java.math.BigDecimal
import java.util.UUID

data class CategorySpendingDto(
    @JsonAlias("category_id")
    val categoryId: UUID,
    @JsonAlias("category_name")
    val categoryName: String,
    @JsonAlias("category_icon")
    val categoryIcon: String,
    val spending: BigDecimal,
    @JsonAlias("percentage_change")
    val percentageChange: BigDecimal,
    @JsonAlias("contribution_percentage")
    val contributionPercentage: BigDecimal
)
