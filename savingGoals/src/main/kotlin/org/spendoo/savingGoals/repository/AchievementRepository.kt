package org.spendoo.savingGoals.repository

import org.spendoo.savingGoals.entity.Achievement
import org.spendoo.savingGoals.entity.AchievementCode
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.*

interface AchievementRepository : JpaRepository<Achievement, UUID> {
    @Query("SELECT a FROM Achievement a WHERE a.id NOT IN (SELECT ua.achievement.id FROM UserAchievement ua WHERE ua.userId = :userId)")
    fun findMissingAchievementsForUser(userId: UUID, pageable: Pageable): Page<Achievement>

    // This query to fetch codes is memory safe because achievements are developer-defined static metadata, not user-generated data, and the total count is small
    @Query("SELECT a.code FROM Achievement a")
    fun findAllCodes(): List<AchievementCode>
}
