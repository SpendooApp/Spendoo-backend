package org.spendoo.notifications.eventListener


import org.slf4j.LoggerFactory
import org.spendoo.events.notifications.PushNotificationEvent
import org.spendoo.events.notifications.UserNotificationsEvent
import org.spendoo.events.notifications.NotificationDetails
import org.spendoo.notifications.service.NotificationService
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component

@Component
class NotificationListener(
    private val notificationService: NotificationService
) {
    private val log = LoggerFactory.getLogger(NotificationListener::class.java)

    @Async
    @EventListener
    fun handleUserNotifications(event: UserNotificationsEvent) {
        try {
            notificationService.saveNotifications(event.notifications)
            log.info("Successfully saved bulk notifications to database.")
        } catch (e: Exception) {
            log.error("Failed to save bulk notifications to database", e)
        }
    }

    @Async
    @EventListener
    fun handlePushNotification(event: PushNotificationEvent) {
        try {
            val notificationDetail = NotificationDetails(
                userId = event.userId,
                subject = event.title,
                message = event.body,
                type = event.type
            )
            notificationService.saveNotification(notificationDetail)
            log.info("Successfully saved push notification to database.")
        } catch (e: Exception) {
            log.error("Failed to save push notification to database", e)
        }
    }
}