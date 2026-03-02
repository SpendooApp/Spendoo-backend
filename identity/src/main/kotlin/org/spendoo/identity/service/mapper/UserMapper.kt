package org.spendoo.identity.service.mapper

import org.spendoo.events.identity.UserCreatedEvent
import org.spendoo.identity.api.dto.request.RegisterRequest
import org.spendoo.identity.entity.Gender
import org.spendoo.identity.entity.User
import java.time.LocalDateTime
import java.util.*
import org.spendoo.events.identity.utils.Gender as EventGender

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

fun User.toUserCreatedEvent(): UserCreatedEvent {
    return UserCreatedEvent(
        id = id,
        password = passwordHash,
        fullName = fullName,
        birthDate = birthDate,
        gender = gender.toEventGender()
    )
}

fun Gender.toEventGender(): EventGender {
    return when (this) {
        Gender.MALE -> EventGender.MALE
        Gender.FEMALE -> EventGender.FEMALE
    }
}