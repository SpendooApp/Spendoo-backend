package org.spendoo.identity.api.dto.request

import jakarta.validation.constraints.NotBlank

data class RefreshTokenRequest(

    @field:NotBlank(message = "Refresh token is required")
    val refreshToken: String
)