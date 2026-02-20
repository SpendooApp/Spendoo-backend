package org.spendoo.identity.dto

data class LoginRequest(
    val email: String,
    val password: String
)