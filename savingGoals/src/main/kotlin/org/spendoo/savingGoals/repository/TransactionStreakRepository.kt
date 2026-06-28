package org.spendoo.savingGoals.repository

import org.spendoo.savingGoals.entity.TransactionStreak
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDate
import java.util.UUID

interface TransactionStreakRepository : JpaRepository<TransactionStreak, UUID> {
    fun existsByUserIdAndStreakDate(userId: UUID, streakDate: LocalDate): Boolean
    fun deleteAllByUserId(userId: UUID)
    fun countByUserId(userId: UUID): Long
}
