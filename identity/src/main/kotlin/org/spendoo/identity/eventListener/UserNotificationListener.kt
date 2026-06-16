package org.spendoo.identity.eventListener

import org.slf4j.LoggerFactory
import org.spendoo.events.notifications.EmailEvent
import org.spendoo.events.notifications.UserNotificationsEvent
import org.spendoo.events.publisher.SpendooEventPublisher
import org.spendoo.identity.service.UserService
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component

@Component
class UserNotificationListener(
    private val userService: UserService,
    private val publisher: SpendooEventPublisher
) {
    private val log = LoggerFactory.getLogger(UserNotificationListener::class.java)

    @Async
    @EventListener
    fun handleUserNotifications(event: UserNotificationsEvent) {
        try {

            val userIds = event.notifications.map { it.userId }.distinct()
            val emailsMap = userService.findEmailsByUserIds(userIds)

            event.notifications.forEach { notification ->

                val userEmail = emailsMap[notification.userId.toString()]

                if(userEmail != null) {
                    publisher.publish(
                        EmailEvent(
                            to = userEmail,
                            subject = notification.subject,
                            text = notification.message
                        )
                    )
                }
                else {
                    log.warn("Email not found for user ID: ${notification.userId}")
                }

            }
        }catch (e: Exception) {
            log.error("Error processing bulk notifications in Identity module", e)
        }
    }
}