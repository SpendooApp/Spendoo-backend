package org.spendoo.app.dto.dashboardDtos

data class DashboardDto(
    val userName: String,
    val totalBalance: Double,
    val expenses: Double,
    val income: Double,
    val offersPreview: List<OfferDto>,
    val goalsPreview: List<SavingsGoalDto>,
    val topSpendingPreview: List<TopSpendingDto>

)
