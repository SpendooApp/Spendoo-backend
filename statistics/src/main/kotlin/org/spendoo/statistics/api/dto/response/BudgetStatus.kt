package org.spendoo.statistics.api.dto.response

import com.fasterxml.jackson.annotation.JsonAlias

enum class BudgetStatus {
    @field:JsonAlias("within") WITHIN,
    @field:JsonAlias("risk") RISK,
    @field:JsonAlias("overspend") OVERSPEND
}
