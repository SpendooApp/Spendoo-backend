package org.spendoo.notifications.integration

import com.google.common.truth.Truth
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.spendoo.notifications.NotificationsTestApplication
import org.spendoo.notifications.entity.NotificationType
import org.spendoo.notifications.repository.NotificationRepository
import org.spendoo.notifications.service.NotificationService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import java.util.UUID

@SpringBootTest(classes = [NotificationsTestApplication::class])
@ActiveProfiles("test")
class NotificationServiceIntegrationTest {

    @Autowired
    private lateinit var notificationService: NotificationService

    @Autowired
    private lateinit var notificationRepository: NotificationRepository

    @BeforeEach
    fun setUp() {
        notificationRepository.deleteAll()
    }

    @Test
    fun `should save notification directly to database`() {

        val userId = UUID.randomUUID()
        val title = "Achievement Unlocked!"
        val message = "You got a new badge."
        val type = NotificationType.ACHIEVEMENT

        notificationService.saveNotification(userId, title, message, type)


        val allNotifications = notificationRepository.findAll()

        Truth.assertThat(allNotifications).hasSize(1)
        Truth.assertThat(allNotifications[0].userId).isEqualTo(userId)
        Truth.assertThat(allNotifications[0].title).isEqualTo(title)
        Truth.assertThat(allNotifications[0].message).isEqualTo(message)
        Truth.assertThat(allNotifications[0].isRead).isFalse()
    }

    @Test
    fun `should count unread notifications correctly`() {

        val userId = UUID.randomUUID()

        notificationService.saveNotification(userId, "T1", "M1", NotificationType.SYSTEM)
        notificationService.saveNotification(userId, "T2", "M2", NotificationType.SYSTEM)
        notificationService.saveNotification(userId, "T3", "M3", NotificationType.SYSTEM)

        val unreadCount = notificationService.getUnreadCount(userId)

        Truth.assertThat(unreadCount).isEqualTo(3)
    }
}