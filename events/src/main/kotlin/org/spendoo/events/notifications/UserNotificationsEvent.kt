package org.spendoo.events.notifications

import org.spendoo.events.SpendooEvent
import org.spendoo.events.notifications.utils.NotificationType
import java.util.UUID

data class NotificationDetails(
    val userId: UUID,
    val subject: String,
    val message: String,
    val type: NotificationType
)

data class UserNotificationsEvent(
    val notifications: List<NotificationDetails>
): SpendooEvent