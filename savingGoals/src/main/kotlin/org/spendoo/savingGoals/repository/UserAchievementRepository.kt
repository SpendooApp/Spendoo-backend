package org.spendoo.savingGoals.repository

import org.spendoo.savingGoals.entity.AchievementType
import org.spendoo.savingGoals.entity.UserAchievement
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.*

interface UserAchievementRepository : JpaRepository<UserAchievement, UUID> {
    fun findAllByUserId(userId: UUID, pageable: Pageable): Page<UserAchievement>

    // This query is memory safe and does not need pagination because there are at most 4 achievements of a specific type
    fun findAllByUserIdAndAchievementAchievementTypeAndIsUnlockedIsFalse(
        userId: UUID,
        achievementType: AchievementType
    ): List<UserAchievement>

    @Query("SELECT DISTINCT ua.userId FROM UserAchievement ua")
    fun findDistinctUserIds(pageable: Pageable): Page<UUID>
}
