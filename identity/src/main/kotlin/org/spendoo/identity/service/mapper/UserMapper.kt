package org.spendoo.identity.service.mapper

import org.spendoo.identity.api.dto.request.RegisterRequest
import org.spendoo.identity.entity.User
import java.time.LocalDateTime
import java.util.UUID

fun RegisterRequest.toEntity(hashedPassword: String, id: UUID = UUID.randomUUID()): User {
    return User(
        id = id,
        fullName = this.fullName,
        email = this.email,
        passwordHash = hashedPassword,
        gender = this.gender,
        birthDate = this.birthDate,
        createdAt = LocalDateTime.now(),
        emailVerifications = emptyList(),
        refreshTokens = emptyList(),
        isVerified = false
    )
}