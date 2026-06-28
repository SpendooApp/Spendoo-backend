package org.spendoo.notifications.entity

import jakarta.persistence.*
import org.spendoo.events.notifications.NotificationDetails
import java.time.LocalDateTime
import java.util.*


@Entity
@Table(name = "notifications", schema = "notification")
data class Notification(
    @Id
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    val id: UUID = UUID.randomUUID(),

    @Column(nullable = false)
    val userId: UUID = UUID.randomUUID(),

    @Column(nullable = false)
    val title: String,

    @Column(nullable = false, length = 500)
    val message: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val type: NotificationType,

    @Column(nullable = false)
    val isRead: Boolean = false,

    @Column(nullable = false)
    val sentAt: LocalDateTime = LocalDateTime.now()

)

fun NotificationDetails.toNotification(): Notification {
    return Notification(
        userId = this.userId,
        title = this.subject,
        message = this.message,
        type = NotificationType.fromStringOrDefault(this.type.name)
    )
}