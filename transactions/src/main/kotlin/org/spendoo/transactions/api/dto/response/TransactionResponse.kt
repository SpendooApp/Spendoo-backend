package org.spendoo.transactions.api.dto.response

import org.spendoo.transactions.entity.Transaction
import org.spendoo.transactions.entity.TransactionType
import java.time.LocalDateTime
import java.util.*

data class TransactionResponse(

    val id: UUID,

    val type: TransactionType,

    val date: LocalDateTime,

    val note: String?,

    val createdAt: LocalDateTime?,

    val entries: List<TransactionEntryResponse>
)

fun Transaction.toResponse(): TransactionResponse {

    return TransactionResponse(
        id = this.id,
        type = this.type,
        date = this.transactionDate,
        note = this.note,
        createdAt = this.createdAt,
        entries = listOf(
            TransactionEntryResponse(
                id = this.id,
                title = this.title,
                amount = this.amount,
                categoryResponse = this.category.toResponse()
        )
        )
    )
}
