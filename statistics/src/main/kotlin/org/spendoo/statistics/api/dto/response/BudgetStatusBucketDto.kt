package org.spendoo.statistics.api.dto.response

import com.fasterxml.jackson.annotation.JsonAlias
import java.math.BigDecimal
import java.time.LocalDateTime

data class BudgetStatusBucketDto(
    val spending: BigDecimal,
    val status: BudgetStatus,
    val percentage: BigDecimal,
    @field:JsonAlias("start_date")
    val startDate: LocalDateTime
)
