package org.spendoo.transactions.api.dto.request

import jakarta.validation.Valid
import jakarta.validation.constraints.*
import org.spendoo.transactions.entity.Category
import org.spendoo.transactions.entity.Transaction
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.*

data class CreateExpenseTransactionRequest(

    @field:NotEmpty(message = "Transaction must have at least one entry")
    @field:Valid
    val entries: List<ExpenseTransactionEntryDto>
)

data class ExpenseTransactionEntryDto(

    @field:NotBlank(message = "Title is required and cannot be empty")
    @field:Size(min = 2, max = 100, message = "Title must be between 2 and 100 characters")
    val title: String,

    @field:Positive(message = "Amount must be greater than 0")
    val amount: BigDecimal,

    val categoryId: UUID,

    @field:PastOrPresent(message = "Date cannot be in the future")
    val transactionDate: LocalDateTime,

    @field:Size(max = 500, message = "Note cannot exceed 500 characters")
    val note: String?,
)

fun ExpenseTransactionEntryDto.toEntity(
    userId: UUID,
    category: Category
): Transaction {
    return Transaction(
        userId = userId,
        amount = -amount,
        transactionDate = transactionDate,
        note = note,
        title = title,
        category = category
    )
}
