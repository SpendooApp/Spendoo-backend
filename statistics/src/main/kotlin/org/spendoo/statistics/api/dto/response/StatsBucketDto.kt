package org.spendoo.statistics.api.dto.response

import com.fasterxml.jackson.annotation.JsonAlias
import java.math.BigDecimal
import java.time.Instant

data class StatsBucketDto(
    val spending: BigDecimal,
    val income: BigDecimal,
    val budget: BigDecimal,
    @JsonAlias("start_date")
    val startDate: Instant,
    @JsonAlias("predicted")
    val predicted: Boolean? = null,
    @JsonAlias("status")
    val status: BudgetStatus? = null
)
