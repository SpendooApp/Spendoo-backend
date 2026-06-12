package org.spendoo.transactions.api.dto.response

import org.springframework.data.domain.Page
import java.math.BigDecimal

data class ScheduledPaymentsDashboardResponse(
    val totalScheduledAmount: BigDecimal,
    val upcomingCount: Long
)