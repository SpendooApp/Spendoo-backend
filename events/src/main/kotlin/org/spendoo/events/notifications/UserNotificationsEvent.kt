package org.spendoo.events.notifications

import org.spendoo.events.SpendooEvent
import java.util.UUID

data class NotificationDetails(
    val userId: UUID,
    val subject: String,
    val message: String
)

data class UserNotificationsEvent(
    val notifications: List<NotificationDetails>
): SpendooEvent