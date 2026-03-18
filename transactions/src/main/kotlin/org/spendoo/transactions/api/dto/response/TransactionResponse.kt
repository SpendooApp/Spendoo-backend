package org.spendoo.transactions.api.dto.response

import org.spendoo.transactions.entity.TransactionType
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

data class TransactionResponse(
    val id: UUID,
    val type: TransactionType,
    val date: LocalDate,
    val note: String?,
    val createdAt: LocalDateTime,
    val entries: List<TransactionEntryResponse>
)