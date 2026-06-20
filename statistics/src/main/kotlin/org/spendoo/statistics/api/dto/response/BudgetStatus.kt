package org.spendoo.statistics.api.dto.response

import com.fasterxml.jackson.annotation.JsonProperty

enum class BudgetStatus {
    @JsonProperty("within") WITHIN,
    @JsonProperty("risk") RISK,
    @JsonProperty("overspend") OVERSPEND
}
