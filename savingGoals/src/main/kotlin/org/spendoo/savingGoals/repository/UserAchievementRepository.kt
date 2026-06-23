package org.spendoo.savingGoals.repository

import org.spendoo.savingGoals.entity.UserAchievement
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface UserAchievementRepository : JpaRepository<UserAchievement, UUID> {
    fun findByUserIdAndAchievementId(userId: UUID, achievementId: UUID): UserAchievement?
    fun findAllByUserId(userId: UUID, pageable: Pageable): Page<UserAchievement>
}