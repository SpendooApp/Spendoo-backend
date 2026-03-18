package org.spendoo.transactions.api.dto.request

import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size
import java.math.BigDecimal
import java.util.UUID

data class TransactionEntryDto(

    @field:NotBlank(message = "Title is required and cannot be empty")
    @field:Size(min = 2, max = 100, message = "Title must be between 2 and 100 characters")
    val title: String,

    @field:NotNull(message = "Amount is required")
    @field:Positive(message = "Amount must be greater than 0")
    val amount: BigDecimal,

    @field:NotNull(message = "Category ID is required")
    val categoryId: UUID
)