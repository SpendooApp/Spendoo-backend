package org.spendoo.app.dto.transactionDtos

data class TransactionUpdateRequest(
    val amount: Double?,
    val category: String?,
    val date: String?,
    val description: String?
)
