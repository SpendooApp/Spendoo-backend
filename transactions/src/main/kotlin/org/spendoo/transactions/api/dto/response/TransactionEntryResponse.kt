package org.spendoo.transactions.api.dto.response

import org.spendoo.transactions.entity.Category
import java.math.BigDecimal
import java.util.UUID

data class TransactionEntryResponse(
    val id: UUID,

    val title: String,

    val amount: BigDecimal,

    val categoryResponse: CategoryResponse
)