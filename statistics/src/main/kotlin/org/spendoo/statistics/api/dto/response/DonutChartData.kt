package org.spendoo.statistics.api.dto.response

import java.math.BigDecimal

data class DonutChartData(
    val totalSpent: BigDecimal,
    val data: List<DonutChartSlice>,
    val image: String
)
