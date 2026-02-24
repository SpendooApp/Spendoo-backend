package org.spendoo.identity.dto.response

data class AuthResponse(
    val accessToken: String,
    val refreshToken: String
)