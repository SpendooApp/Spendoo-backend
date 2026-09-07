package org.spendoo.statistics.api.dto.response

import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonProperty

data class CombinedStatsResponse(
    @JsonProperty("financialStats")
    @JsonAlias("financial_stats_forecast", "financial_stats")
    val financialStats: FinancialStatsResponse,
    @JsonProperty("budgetStatus")
    @JsonAlias("budget_status")
    val budgetStatus: BudgetStatusResponse,
    @JsonProperty("topCategories")
    @JsonAlias("top_categories")
    val topCategories: TopCategoriesResponse
)


