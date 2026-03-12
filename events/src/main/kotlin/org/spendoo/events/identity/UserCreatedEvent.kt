package org.spendoo.events.identity

import org.spendoo.events.SpendooEvent
import org.spendoo.events.identity.utils.Gender
import java.time.LocalDate
import java.util.*

data class UserCreatedEvent(
    val id: UUID,
    val password: String,
    val fullName: String,
    val birthDate: LocalDate,
    val gender: Gender,
    val imageUrl: String?
) : SpendooEvent