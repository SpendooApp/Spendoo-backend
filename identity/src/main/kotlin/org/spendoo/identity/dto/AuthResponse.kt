package org.spendoo.identity.dto

data class AuthResponse(
    val token: String,
    val message: String
)