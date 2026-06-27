package org.spendoo.events.achievements

import org.spendoo.events.SpendooEvent
import org.spendoo.events.achievements.utils.AchievementType
import java.math.BigDecimal
import java.util.*

data class AchievementEarnedEvent(
    val achievementId: UUID,
    val userId: UUID,
    val titleEn: String,
    val titleAr: String,
    val descriptionEn: String,
    val descriptionAr: String,
    val level: Int,
    val targetValue: BigDecimal,
    val achievementType: AchievementType
) : SpendooEvent

