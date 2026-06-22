package org.spendoo.transactions.api.dto.response

import java.math.BigDecimal
import java.time.LocalDateTime

data class BudgetIntervalDto(
    val amount: BigDecimal,
    val startDate: LocalDateTime,
    val endDate: LocalDateTime
)
