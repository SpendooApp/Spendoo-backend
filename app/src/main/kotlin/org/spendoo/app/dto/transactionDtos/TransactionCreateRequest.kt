package org.spendoo.app.dto.transactionDtos

data class TransactionCreateRequest(
    val type: String,
    val amount: List<Double>,
    val category: List<String>,
    val date: String,
    val description: String? = null
)