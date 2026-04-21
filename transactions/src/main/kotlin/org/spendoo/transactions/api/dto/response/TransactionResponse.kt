package org.spendoo.transactions.api.dto.response

import org.spendoo.transactions.entity.Transaction
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.*

data class TransactionResponse(

    val id: UUID,
    val title: String,
    val amount: BigDecimal,
    val note: String?,
    val date: LocalDateTime,
    val categoryResponse: CategoryResponseWithBudget?,
)

fun Transaction.toResponse(): TransactionResponse {
    return TransactionResponse(
        id = this.id,
        date = this.transactionDate,
        note = this.note,
        title = this.title,
        amount = this.amount,
        categoryResponse = category?.toResponseWithBudget()
    )
}
