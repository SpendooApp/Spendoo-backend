package org.spendoo.statistics.api.dto.response

import java.math.BigDecimal

data class LineChartData(
    val labels: List<String>,
    val budgetData: List<BigDecimal>,
    val spentData: List<BigDecimal>,
    val forecastData: List<BigDecimal>,
    val maxSpentValue: BigDecimal,
    val maxSpentPosition: Int,
    val image: String
)
