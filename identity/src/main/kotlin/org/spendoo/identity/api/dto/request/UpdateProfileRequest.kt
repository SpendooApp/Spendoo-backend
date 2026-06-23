package org.spendoo.identity.api.dto.request

import org.spendoo.identity.entity.Gender
import jakarta.validation.constraints.Past
import java.time.LocalDate

data class UpdateProfileRequest(
    val fullName: String,
    val gender: Gender,
    @field:Past(message = "Birth date must be in the past")
    val birthDate: LocalDate
)