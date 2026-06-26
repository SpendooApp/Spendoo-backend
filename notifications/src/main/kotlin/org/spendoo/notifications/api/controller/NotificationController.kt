package org.spendoo.notifications.api.controller

import org.spendoo.notifications.entity.Notification
import org.spendoo.notifications.service.NotificationService
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1/notifications")
class NotificationController(
    private val notificationService: NotificationService
) {

    @GetMapping
    fun getAllNotifications(
        @AuthenticationPrincipal userId: UUID,
        pageable: Pageable
    ): ResponseEntity<Page<Notification>> {
        return ResponseEntity.ok(notificationService.getUserNotifications(userId, pageable))
    }

    @GetMapping("/unread-count")
    fun getUnreadCount(
        @AuthenticationPrincipal userId: UUID
    ): ResponseEntity<Map<String, Long>> {
        val count = notificationService.getUnreadCount(userId)
        return ResponseEntity.ok(mapOf("unreadCount" to count))
    }

    @PatchMapping("/mark-all-read")
    fun markAllAsRead(
        @AuthenticationPrincipal userId: UUID
    ): ResponseEntity<Unit> {
        notificationService.markAllAsRead(userId)
        return ResponseEntity.ok().build()
    }

}