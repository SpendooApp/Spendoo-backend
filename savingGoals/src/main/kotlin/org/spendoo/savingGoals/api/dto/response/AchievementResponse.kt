package org.spendoo.savingGoals.api.dto.response

import org.spendoo.savingGoals.entity.UserAchievement
import java.math.BigDecimal
import java.util.*

data class AchievementResponse(
    val id: UUID,
    val title: String,
    val description: String,
    val targetValue: BigDecimal,
    val currentProgress: BigDecimal,
    val isUnlocked: Boolean,
)

fun UserAchievement.toResponse(languageCode: String): AchievementResponse {
    val useArabic = languageCode.lowercase().startsWith("ar")
    return AchievementResponse(
        id = this.id,
        title = if (useArabic) this.achievement.titleAr else this.achievement.titleEn,
        description = if (useArabic) this.achievement.descriptionAr else this.achievement.descriptionEn,
        targetValue = this.achievement.targetValue,
        currentProgress = this.currentProgress,
        isUnlocked = this.isUnlocked
    )
}
