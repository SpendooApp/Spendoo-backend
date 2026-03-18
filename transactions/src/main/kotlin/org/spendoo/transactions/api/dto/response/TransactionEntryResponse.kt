package org.spendoo.transactions.api.dto.response

import java.math.BigDecimal
import java.util.UUID

data class TransactionEntryResponse(
    val id: UUID,
    val title: String,
    val amount: BigDecimal,
    val category: CategoryResponse
)