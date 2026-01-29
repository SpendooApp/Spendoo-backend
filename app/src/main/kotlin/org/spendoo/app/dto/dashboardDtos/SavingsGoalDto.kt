package org.spendoo.app.dto.dashboardDtos

data class SavingsGoalDto(
    val id: Long,
    val name: String,
    val targetAmount: Double,
    val savedAmount: Double
)
