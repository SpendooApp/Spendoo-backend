package org.spendoo.statistics.api.dto.response

import org.spendoo.statistics.model.BarStatus
import java.math.BigDecimal

data class BarChartPeriodItem(
    val periodLabel: String,
    val spent: BigDecimal,
    val budget: BigDecimal,
    val ratio: BigDecimal,
    val status: BarStatus
)
