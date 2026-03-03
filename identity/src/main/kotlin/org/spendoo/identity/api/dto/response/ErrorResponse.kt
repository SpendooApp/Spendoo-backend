package org.spendoo.identity.api.dto.response

data class ErrorResponse(
    val message: String,
    val status: Int
)