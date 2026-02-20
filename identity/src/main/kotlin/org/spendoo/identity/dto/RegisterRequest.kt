package org.spendoo.identity.dto

data class RegisterRequest(
    val fullName: String,
    val email: String,
    val password: String,
    val gender: String,
    val age: Int
)