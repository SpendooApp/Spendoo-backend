package org.spendoo.events.notifications

import org.spendoo.events.SpendooEvent
import org.spendoo.events.notifications.utils.NotificationMedium
import org.spendoo.events.notifications.utils.NotificationType
import java.util.*

data class NotificationDetails(
    val userId: UUID,
    val subject: String,
    val message: String,
    val type: NotificationType,
    val medium: NotificationMedium = NotificationMedium.EMAIL
)

data class UserNotificationsEvent(
    val notifications: List<NotificationDetails>
) : SpendooEvent