package org.spendoo.transactions.api.dto.response

import java.math.BigDecimal

data class BalanceSummary(
    val totalBalance: BigDecimal,
    val income: BigDecimal,
    val expenses: BigDecimal
)