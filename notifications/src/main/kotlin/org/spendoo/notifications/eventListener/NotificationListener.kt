package org.spendoo.notifications.eventListener


import org.slf4j.LoggerFactory
import org.spendoo.events.notifications.UserNotificationsEvent
import org.spendoo.notifications.service.NotificationService
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component

@Component
class NotificationListener(
    private val notificationService: NotificationService
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Async
    @EventListener
    fun handleUserNotifications(event: UserNotificationsEvent) {
        try {
            event.notifications.forEach { detail ->
                notificationService.saveNotification(
                    userId = detail.userId,
                    title = detail.subject,
                    message = detail.message,
                    type = org.spendoo.notifications.entity.NotificationType.valueOf(detail.type.name)
                )
            }
            log.info("Successfully saved ${event.notifications.size} in-app notifications to database.")
        } catch (e: Exception) {
            log.error("Failed to save in-app notifications to database", e)
        }
    }
}