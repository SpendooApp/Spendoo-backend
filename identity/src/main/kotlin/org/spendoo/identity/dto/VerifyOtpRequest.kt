package org.spendoo.identity.dto

data class VerifyOtpRequest(
    val email: String,
    val otp: String
)
