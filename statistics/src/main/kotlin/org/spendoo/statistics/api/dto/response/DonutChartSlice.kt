package org.spendoo.statistics.api.dto.response

import java.math.BigDecimal

data class DonutChartSlice(
    val categoryName: String,
    val amount: BigDecimal,
    val percentage: BigDecimal
)
