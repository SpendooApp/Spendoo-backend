package org.spendoo.identity.api.controller

data class ProfileResponse(
    val id: String,
    val fullName: String,
    val birthDate: String,
    val gender: String,
    val imageUrl: String?
)