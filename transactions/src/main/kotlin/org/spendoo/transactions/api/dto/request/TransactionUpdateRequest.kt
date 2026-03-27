package org.spendoo.transactions.api.dto.request

import jakarta.validation.constraints.*
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

data class TransactionUpdateRequest(

    @field:Pattern(regexp = ".*\\S.*", message = "Transaction title must not be blank")
    val title: String,

    @field:PastOrPresent(message = "Date cannot be in the future")
    val transactionDate: LocalDateTime?,

    @field:Size(max = 500, message = "Note cannot exceed 500 characters")
    val note: String?,

    @field:NotNull(message = "Amount is required")
    @field:Positive(message = "Amount must be greater than 0")
    val amount: BigDecimal?,

    val categoryId: UUID?,
)
