package org.spendoo.app.dto.transactionDtos

data class TransactionDto(
    val id: Long,
    val type: String,   // "expense" | "income"
    val amount: List<Double>,
    val category: List<String>,
    val date: String,
    val createdAt: String,
    val description: String? = null
)
