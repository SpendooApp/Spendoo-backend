package org.spendoo.events.achievements

import org.spendoo.events.SpendooEvent
import org.spendoo.events.achievements.utils.AchievementType
import java.util.*

data class AchievementEarnedEvent(
    val achievementId: UUID,
    val userId: UUID,
    val titleEn: String,
    val descriptionEn: String,
    val level: Int,
    val achievementType: AchievementType
) : SpendooEvent
