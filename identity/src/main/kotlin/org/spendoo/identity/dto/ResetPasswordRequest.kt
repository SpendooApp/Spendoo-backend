package org.spendoo.identity.dto

data class ResetPasswordRequest(
    val email: String,
    val otp: String,
    val newPassword: String
)