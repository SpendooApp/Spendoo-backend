package org.spendoo.statistics.api.dto.response

import com.fasterxml.jackson.annotation.JsonAlias

data class CombinedStatsResponse(
    @field:JsonAlias("financial_stats_forecast")
    val financialStats: FinancialStatsResponse,
    @field:JsonAlias("budget_status")
    val budgetStatus: BudgetStatusResponse,
    @field:JsonAlias("top_categories")
    val topCategories: TopCategoriesResponse
)
