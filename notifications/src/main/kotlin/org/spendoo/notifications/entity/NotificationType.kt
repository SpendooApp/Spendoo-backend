package org.spendoo.notifications.entity

enum class NotificationType {
    ACHIEVEMENT,
    GOAL,
    USER_FOLLOW,
    PAYMENT_REMINDER,
    SYSTEM;

    companion object {
        fun fromStringOrDefault(type: String): NotificationType {
            return entries.firstOrNull { it.name.equals(type, ignoreCase = true) } ?: SYSTEM
        }
    }
}