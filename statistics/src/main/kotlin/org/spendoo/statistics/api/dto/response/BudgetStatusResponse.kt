package org.spendoo.statistics.api.dto.response

import com.fasterxml.jackson.annotation.JsonAlias
import java.math.BigDecimal

data class BudgetStatusResponse(
    val buckets: List<BudgetStatusBucketDto>,
    @JsonAlias("highest_spending")
    val highestSpending: BigDecimal
)
