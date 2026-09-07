package org.spendoo.statistics.api.dto.response

import com.fasterxml.jackson.annotation.JsonAlias
import java.math.BigDecimal

data class FinancialStatsResponse(
    val buckets: List<StatsBucketDto>,
    @JsonAlias("highest_spending_bucket_index")
    val highestSpendingBucketIndex: Int,
    @JsonAlias("highest_value")
    val highestValue: BigDecimal,
    @JsonAlias("predict")
    val predicted: Boolean? = null
)
