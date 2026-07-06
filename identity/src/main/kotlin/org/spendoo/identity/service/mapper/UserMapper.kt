package org.spendoo.identity.service.mapper

import org.spendoo.events.identity.UserCreatedEvent
import org.spendoo.events.identity.UserUpdatedEvent
import org.spendoo.identity.api.dto.request.RegisterRequest
import org.spendoo.identity.api.dto.response.ProfileResponse
import org.spendoo.identity.entity.Gender
import org.spendoo.identity.entity.PlanCode
import org.spendoo.identity.entity.User
import java.time.Instant
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
        createdAt = Instant.now(),
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
        gender = gender.toEventGender(),
        imageUrl = imageUrl,
    )
}

fun User.toUserUpdatedEvent(): UserUpdatedEvent {
    return UserUpdatedEvent(
        id = id,
        password = passwordHash,
        fullName = fullName,
        birthDate = birthDate,
        gender = gender.toEventGender(),
        imageUrl = imageUrl,
    )
}

fun User.toProfileResponse(imageBaseUrl: String, currentPlan: String, planCode: PlanCode,followCode: String): ProfileResponse {
    val resolvedImageUrl = if (imageUrl.isNullOrBlank()) {
        null
    } else {
        "$imageBaseUrl/$imageUrl"
    }
    return ProfileResponse(
        id = id.toString(),
        fullName = fullName,
        email = email,
        birthDate = birthDate.toString(),
        gender = gender.name,
        imageUrl = resolvedImageUrl,
        currentPlan = currentPlan,
        planCode = planCode,
        followCode = followCode
    )
}

fun Gender.toEventGender(): EventGender {
    return when (this) {
        Gender.MALE -> EventGender.MALE
        Gender.FEMALE -> EventGender.FEMALE
    }
}