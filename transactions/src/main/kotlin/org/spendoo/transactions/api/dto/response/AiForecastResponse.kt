package org.spendoo.transactions.api.dto.response

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.math.BigDecimal

@JsonIgnoreProperties(ignoreUnknown = true)
data class AiForecastResponse(
    val predict: Boolean,
    val buckets: List<ForecastBucket> = emptyList()
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class ForecastBucket(
    val predicted: Boolean,
    val spending: BigDecimal,
    val budget: BigDecimal,
    @field:JsonProperty("start_date")
    val startDate: String
)