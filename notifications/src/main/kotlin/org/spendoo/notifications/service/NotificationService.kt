package org.spendoo.notifications.service

import org.spendoo.notifications.entity.Notification
import org.spendoo.notifications.entity.NotificationType
import org.spendoo.notifications.repository.NotificationRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class NotificationService(
    private val notificationRepository: NotificationRepository
) {

    @Transactional
    fun saveNotification(userId: UUID, title: String, message: String, type: NotificationType) {
        val notification = Notification(
            userId = userId,
            title = title,
            message = message,
            type = type
        )
        notificationRepository.save(notification)
    }

    @Transactional(readOnly = true)
    fun getUserNotifications(userId: UUID, pageable: Pageable): Page<Notification> {
        return notificationRepository.findAllByUserIdOrderBySentAtDesc(userId, pageable)
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