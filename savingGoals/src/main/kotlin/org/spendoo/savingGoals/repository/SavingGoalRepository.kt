package org.spendoo.savingGoals.repository

import org.spendoo.savingGoals.entity.SavingGoal
import org.spendoo.savingGoals.service.model.GoalParams
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.math.BigDecimal
import java.util.*

interface SavingGoalRepository : JpaRepository<SavingGoal, UUID> {

    fun findByIdAndUserId(id: UUID, userId: UUID): SavingGoal?

    fun deleteByIdAndUserId(id: UUID, userId: UUID): Int

    @Query(
        """
        SELECT NEW org.spendoo.savingGoals.service.model.GoalParams(
            g.id,
            g.goalName,
            g.priority,
            g.goalIcon,
            g.deadline,
            SUM(h.amount),
            g.targetAmount,
            g.isCompleted
        )
        FROM SavingGoal g
        LEFT JOIN g.savingGoalHistory h ON g.id = h.goal.id 
        WHERE g.id = :id AND g.userId = :userId
        GROUP BY
            g.id,
            g.goalName,
            g.priority,
            g.goalIcon,
            g.deadline,
            g.targetAmount,
            g.isCompleted
        """
    )
    fun findGoalWithSavedAmount(
        id: UUID,
        userId: UUID
    ): GoalParams?

    @Query(
        """
    SELECT NEW org.spendoo.savingGoals.service.model.GoalParams(
        g.id,
        g.goalName,
        g.priority,
        g.goalIcon,
        g.deadline,
        SUM(h.amount),
        g.targetAmount,
        g.isCompleted
    )
    FROM SavingGoal g
    LEFT JOIN g.savingGoalHistory h
    ON g.id = h.goal.id 
    WHERE g.userId = :userId
    GROUP BY
        g.id,
        g.goalName,
        g.priority,
        g.goalIcon,
        g.deadline,
        g.targetAmount,
        g.isCompleted
"""
    )
    fun findAllByUserId(
        userId: UUID,
        pageable: Pageable
    ): Page<GoalParams>

    @Query(
        """
    SELECT 
    SUM(h.amount) 
    FROM SavingGoalHistory h 
    JOIN h.goal g
    ON g.id = h.goal.id
    WHERE g.userId = :userId 
    GROUP BY g.userId
            
        """
    )
    fun sumSavedAmountByUserId(userId: UUID): BigDecimal?

    @Query("""SELECT SUM(g.targetAmount) FROM SavingGoal g WHERE g.userId = :userId """)
    fun sumTargetAmountByUserId(userId: UUID): BigDecimal?

    fun countByUserIdAndIsCompletedTrue(userId: UUID): Long

    @Query(
        """
    SELECT COUNT(g) 
    FROM SavingGoal g
    WHERE g.userId = :userId 
      AND g.isCompleted = true 
      AND g.priority >= 3
    """
    )
    fun countCompletedHighPriorityGoals(userId: UUID): Long
}