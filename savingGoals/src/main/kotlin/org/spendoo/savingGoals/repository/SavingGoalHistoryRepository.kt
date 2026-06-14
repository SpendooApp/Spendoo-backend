package org.spendoo.savingGoals.repository

import org.spendoo.savingGoals.entity.SavingGoalHistory
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.math.BigDecimal
import java.util.UUID

interface SavingGoalHistoryRepository : JpaRepository<SavingGoalHistory, UUID> {
    @Query("""
    SELECT COALESCE(SUM(h.amount), 0)
    FROM SavingGoalHistory h
    WHERE h.goal.id = :goalId
""")
    fun getCurrentAmountByGoalId(
        @Param("goalId") goalId: UUID
    ): BigDecimal

}