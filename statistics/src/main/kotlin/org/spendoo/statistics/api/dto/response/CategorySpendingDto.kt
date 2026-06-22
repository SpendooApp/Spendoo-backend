package org.spendoo.statistics.api.dto.response

import com.fasterxml.jackson.annotation.JsonAlias
import java.math.BigDecimal
import java.util.UUID

data class CategorySpendingDto(
    @field:JsonAlias("category_id")
    val categoryId: UUID,
    @field:JsonAlias("category_name")
    val categoryName: String,
    @field:JsonAlias("category_icon")
    val categoryIcon: String,
    val spending: BigDecimal,
    @field:JsonAlias("percentage_change")
    val percentageChange: BigDecimal,
    @field:JsonAlias("contribution_percentage")
    val contributionPercentage: BigDecimal
)
