package org.spendoo.statistics.api.dto.response

import com.fasterxml.jackson.annotation.JsonAlias
import java.math.BigDecimal
import java.time.LocalDateTime

data class StatsBucketDto(
    val spending: BigDecimal,
    val income: BigDecimal,
    val budget: BigDecimal,
    @field:JsonAlias("start_date")
    val startDate: LocalDateTime,
    @field:JsonAlias("predicted")
    val isPredicted: Boolean,
    @field:JsonAlias("status")
    val status: BudgetStatus? = null
)
