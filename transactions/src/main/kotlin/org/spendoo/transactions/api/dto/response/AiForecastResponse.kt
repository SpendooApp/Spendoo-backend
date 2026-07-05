package org.spendoo.transactions.api.dto.response

import com.fasterxml.jackson.annotation.JsonProperty
import java.math.BigDecimal

data class AiForecastResponse(
    val predict: Boolean,
    val buckets: List<ForecastBucket> = emptyList()
)

data class ForecastBucket(
    val predicted: Boolean,
    val spending: BigDecimal,
    val budget: BigDecimal,
    @field:JsonProperty("start_date")
    val startDate: String
)