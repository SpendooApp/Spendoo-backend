package org.spendoo.identity.response


data class ErrorResponse(
    val message: String,
    val status: Int
)