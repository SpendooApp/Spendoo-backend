package org.spendoo.notifications.eventListener

import org.spendoo.events.achievements.AchievementEarnedEvent
import org.spendoo.events.notifications.NotificationDetails
import org.spendoo.events.notifications.UserNotificationsEvent
import org.spendoo.events.notifications.utils.NotificationMedium
import org.spendoo.events.notifications.utils.NotificationType
import org.spendoo.events.publisher.SpendooEventPublisher
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

@Component
class AchievementNotificationListener(
    private val eventPublisher: SpendooEventPublisher
) {

    @EventListener
    fun handleAchievementEarned(event: AchievementEarnedEvent) {
        val customizedMessage = """
            Congratulations!🎉 You've unlocked the Level ${event.level} badge: "${event.titleEn}".
            
            Every small step forward brings you closer to your financial dreams. 
            
            Keep up the incredible work and continue mastering your money with Spendoo! 🚀
        """.trimIndent()

        val notificationDetail = NotificationDetails(
            userId = event.userId,
            subject = "🏆 Badge Earned: ${event.titleEn}! ",
            message = customizedMessage,
            type = NotificationType.ACHIEVEMENT,
            medium = NotificationMedium.PUSH
        )

        eventPublisher.publish(
            UserNotificationsEvent(
                notifications = listOf(notificationDetail)
            )
        )
    }
}