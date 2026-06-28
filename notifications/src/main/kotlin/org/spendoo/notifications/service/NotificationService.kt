package org.spendoo.notifications.service

import org.spendoo.events.notifications.NotificationDetails
import org.spendoo.notifications.api.dto.response.NotificationResponse
import org.spendoo.notifications.entity.toNotification
import org.spendoo.notifications.repository.NotificationRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
class NotificationService(
    private val notificationRepository: NotificationRepository
) {

    @Transactional
    fun saveNotification(notification: NotificationDetails) {
        val notification = notification.toNotification()
        notificationRepository.save(notification)
    }
    
    @Transactional
    fun saveNotifications(notifications: List<NotificationDetails>) {
        val notifications = notifications.map(NotificationDetails::toNotification)
        notificationRepository.saveAll(notifications)
    }

    @Transactional(readOnly = true)
    fun getUserNotifications(userId: UUID, pageable: Pageable): Page<NotificationResponse> {
        val notifications =  notificationRepository.findAllByUserIdOrderBySentAtDesc(userId, pageable)

        return notifications.map { notification ->
            NotificationResponse(
                id = notification.id,
                title = notification.title,
                message = notification.message,
                type = notification.type,
                sentAt = notification.sentAt,
                isRead = notification.isRead
            )
        }
    }

    @Transactional(readOnly = true)
    fun getUnreadCount(userId: UUID): Long {
        return notificationRepository.countByUserIdAndIsReadFalse(userId)
    }

    @Transactional
    fun markAllAsRead(userId: UUID) {
        notificationRepository.markAllAsReadByUserId(userId)
    }
}