package org.spendoo.categories.api.dto.response

import java.time.LocalDate

data class BudgetResponse(

    val amount: Double,

    val spentAmount: Double,

    val spendingPercentage: Int,

    val startDate: LocalDate,

    val endDate: LocalDate
)
