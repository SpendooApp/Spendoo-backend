package org.spendoo.transactions.api.dto.request

import jakarta.validation.constraints.PastOrPresent
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size
import org.spendoo.transactions.entity.Category
import org.spendoo.transactions.entity.Transaction
import java.math.BigDecimal
import java.time.Instant
import java.util.*

data class TransactionUpdateRequest(

    @field:Pattern(regexp = ".*\\S.*", message = "Transaction title must not be blank")
    val title: String,

    @field:PastOrPresent(message = "Date cannot be in the future")
    val transactionDate: Instant,

    @field:Size(max = 500, message = "Note cannot exceed 500 characters")
    val note: String?,

    @field:Positive(message = "Amount must be greater than 0")
    val amount: BigDecimal,

    val categoryId: UUID?,
)


fun TransactionUpdateRequest.toEntity(
    transactionId: UUID,
    userId: UUID,
    category: Category?
): Transaction {
    return Transaction(
        id = transactionId,
        userId = userId,
        amount = category?.let { -amount } ?: amount,
        transactionDate = transactionDate,
        note = note,
        title = title,
        category = category
    )
}
