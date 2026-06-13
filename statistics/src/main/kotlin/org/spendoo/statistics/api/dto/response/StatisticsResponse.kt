package org.spendoo.statistics.api.dto.response

data class StatisticsResponse(
    val lineChart: LineChartData,
    val barChart: BarChartData,
    val donutChart: DonutChartData,
    val topCategories: List<TopCategoryDto>
)
