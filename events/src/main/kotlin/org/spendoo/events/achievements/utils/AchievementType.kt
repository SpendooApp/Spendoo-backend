package org.spendoo.events.achievements.utils

enum class AchievementType {
    SAVINGS,
    GOALS,
    LOGIN_STREAK,
    TRANSACTION_STREAK,
    CATEGORIES;

    companion object {
        fun fromStringOrDefault(type: String): AchievementType {
            return entries.firstOrNull { it.name.equals(type, ignoreCase = true) } ?: SAVINGS
        }
    }
}