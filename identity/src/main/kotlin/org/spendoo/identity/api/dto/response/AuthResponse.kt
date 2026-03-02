package org.spendoo.identity.api.dto.response

data class AuthResponse(
    val accessToken: String,
    val refreshToken: String
)