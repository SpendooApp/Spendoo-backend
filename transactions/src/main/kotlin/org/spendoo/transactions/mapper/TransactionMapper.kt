package org.spendoo.transactions.mapper

import org.spendoo.transactions.api.dto.request.CreateTransactionRequest
import org.spendoo.transactions.api.dto.request.TransactionEntryDto
import org.spendoo.transactions.entity.Category
import org.spendoo.transactions.entity.Transaction
import java.util.UUID

fun TransactionEntryDto.toEntity(
    userId: UUID, request: CreateTransactionRequest, category: Category): Transaction
{
    return Transaction(
        userId = userId,
        type = request.type,
        amount = this.amount,
        transactionDate = request.transactionDate,
        note = request.note,
        title = this.title,
        category = category
    )
}
