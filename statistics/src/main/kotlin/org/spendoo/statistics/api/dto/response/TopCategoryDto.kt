package org.spendoo.statistics.api.dto.response

import org.spendoo.statistics.model.TrendDirection
import org.spendoo.transactions.entity.CategoryIcon
import java.math.BigDecimal

data class TopCategoryDto(
    val categoryName: String,
    val categoryIcon: CategoryIcon,
    val amount: BigDecimal,
    val percentageChange: BigDecimal,
    val trend: TrendDirection
)
