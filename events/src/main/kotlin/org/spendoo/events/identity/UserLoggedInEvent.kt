package org.spendoo.events.identity

import org.spendoo.events.SpendooEvent
import java.util.UUID

data class UserLoggedInEvent(val userId: UUID) : SpendooEvent
