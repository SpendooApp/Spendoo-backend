package org.spendoo.transactions.api.dto.request

import jakarta.validation.Valid
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.PastOrPresent
import jakarta.validation.constraints.Size
import org.spendoo.transactions.entity.TransactionType
import java.time.LocalDate

data class CreateTransactionRequest(

    @field:NotNull(message = "Transaction type is required")
    val type: TransactionType,

    @field:PastOrPresent(message = "Date cannot be in the future")
    val date: LocalDate,

    @field:Size(max = 500, message = "Note cannot exceed 500 characters")
    val note: String?,

    @field:NotEmpty(message = "Transaction must have at least one entry")
    @field:Valid
    val entries: List<TransactionEntryDto>
)