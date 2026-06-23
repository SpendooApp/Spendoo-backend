package org.spendoo.identity.api.dto.response

import java.util.UUID

data class UserSearchResponse(
    val userId: UUID,
    val fullName: String,
    val imageUrl: String?
)