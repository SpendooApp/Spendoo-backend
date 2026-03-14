package org.spendoo.categories.api.dto.response

import java.math.BigDecimal

data class CategoriesOverViewResponse(
    val totalIncome: BigDecimal,

    val categorizedAmount: BigDecimal,

    val remainingAmount: BigDecimal,
)
