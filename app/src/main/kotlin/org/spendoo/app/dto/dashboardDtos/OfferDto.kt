package org.spendoo.app.dto.dashboardDtos


data class OfferDto(
    val id: Long,
    val title: String,
    val description: String,
    val discountPercent: Int
)
