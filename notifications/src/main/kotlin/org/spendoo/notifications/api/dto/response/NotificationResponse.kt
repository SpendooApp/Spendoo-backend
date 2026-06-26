package org.spendoo.notifications.api.dto.response

import org.spendoo.notifications.entity.NotificationType
import java.time.LocalDateTime
import java.util.UUID

data class NotificationResponse(
    val id: UUID,
    val title: String,
    val message: String,
    val type: NotificationType,
    val sentAt: LocalDateTime,
    val isRead: Boolean
)