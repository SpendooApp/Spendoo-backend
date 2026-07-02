package org.spendoo.statistics.api.dto.response

import com.fasterxml.jackson.annotation.JsonAlias
import java.math.BigDecimal

data class FinancialStatsResponse(
    val buckets: List<StatsBucketDto>,
    @field:JsonAlias("highest_spending_bucket_index")
    val highestSpendingBucketIndex: Int,
    @field:JsonAlias("highest_value")
    val highestValue: BigDecimal,
    @field:JsonAlias("predict")
    val isPredicted: Boolean
)
