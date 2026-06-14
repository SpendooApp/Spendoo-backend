package org.spendoo.transactions.api.dto.response

import org.spendoo.transactions.entity.Transaction
import org.spendoo.transactions.entity.TransactionType
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.*

data class TransactionResponse(

    val id: UUID,
    val title: String,
    val amount: BigDecimal,
    val note: String?,
    val transactionDate: LocalDateTime,
    val categoryResponse: CategoryResponseWithBudget?,
    val type: TransactionType,
)

fun Transaction.toResponse(): TransactionResponse {
    return TransactionResponse(
        id = this.id,
        transactionDate = this.transactionDate,
        note = this.note,
        title = this.title,
        amount = this.amount,
        categoryResponse = category?.toResponseWithBudget(),
        type = if (this.amount >= BigDecimal.ZERO) TransactionType.INCOME else TransactionType.EXPENSE
    )
}

fun org.spendoo.transactions.entity.TransactionView.toResponse(): TransactionResponse {
    return TransactionResponse(
        id = this.id,
        transactionDate = this.transactionDate,
        note = this.note,
        title = this.title,
        amount = this.amount,
        categoryResponse = category?.toResponseWithBudget(),
        type = this.type
    )
}
