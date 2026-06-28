package org.spendoo.savingGoals.repository

import org.spendoo.savingGoals.entity.LoginStreak
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDate
import java.util.UUID

interface LoginStreakRepository : JpaRepository<LoginStreak, UUID> {
    fun existsByUserIdAndStreakDate(userId: UUID, streakDate: LocalDate): Boolean
    fun deleteAllByUserId(userId: UUID)
    fun countByUserId(userId: UUID): Long
}
