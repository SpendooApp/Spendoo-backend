package org.spendoo.transactions.api.dto.response

import java.math.BigDecimal
import java.time.Instant

data class BudgetIntervalDto(
    val amount: BigDecimal,
    val startDate: Instant,
    val endDate: Instant
)
