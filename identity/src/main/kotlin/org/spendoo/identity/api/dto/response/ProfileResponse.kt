package org.spendoo.identity.api.dto.response

import org.spendoo.identity.entity.PlanCode

data class ProfileResponse(
    val id: String,
    val fullName: String,
    val email: String,
    val birthDate: String,
    val gender: String,
    val imageUrl: String?,
    val currentPlan: String,
    val planCode: PlanCode,
    val followCode: String
)