package org.spendoo.events.notifications

import org.spendoo.events.SpendooEvent
import org.spendoo.events.notifications.utils.NotificationType
import java.util.UUID

data class PushNotificationEvent(
    val userId: UUID,
    val tokens: List<String>,
    val title: String,
    val body: String,
    val type: NotificationType,
    val dataPayload: Map<String, String> = emptyMap()
) : SpendooEvent
