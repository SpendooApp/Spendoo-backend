package org.spendoo.app.dto.notificationDtos

data class NotificationDto(
    val id: Long,
    val title: String,
    val message: String,
    val createdAt: String,
    val read: Boolean
)
