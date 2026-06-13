package org.spendoo.statistics.api.dto.response

data class BarChartData(
    val labels: List<String>,
    val data: List<BarChartPeriodItem>,
    val image: String
)
