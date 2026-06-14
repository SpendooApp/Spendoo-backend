package org.spendoo.savingGoals.service

import org.spendoo.savingGoals.api.dto.request.AssignAmountRequest
import org.spendoo.savingGoals.api.dto.request.GoalCreateRequest
import org.spendoo.savingGoals.api.dto.request.GoalUpdateRequest
import org.spendoo.savingGoals.api.dto.request.toEntity
import org.spendoo.savingGoals.api.dto.response.GoalResponse
import org.spendoo.savingGoals.api.dto.response.GoalsSummary
import org.spendoo.savingGoals.api.dto.response.toResponse
import org.spendoo.savingGoals.entity.SavingBalance
import org.spendoo.savingGoals.entity.SavingGoal
import org.spendoo.savingGoals.entity.SavingGoalHistory
import org.spendoo.savingGoals.repository.SavingBalanceRepository
import org.spendoo.savingGoals.repository.SavingGoalHistoryRepository
import org.spendoo.savingGoals.repository.SavingGoalRepository
import org.spendoo.savingGoals.service.model.GoalParams
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.util.*

@Service
class SavingGoalService(
    private val savingGoalRepository: SavingGoalRepository,
    private val savingBalanceRepository: SavingBalanceRepository,
    private val savingGoalHistoryRepository: SavingGoalHistoryRepository
) {
    @Transactional
    fun createSavingGoal(request: GoalCreateRequest, userId: UUID) {
        val goal = request.toEntity(userId)
        savingGoalRepository.save(goal)
    }

    @Transactional
    fun updateGoal(goalId: UUID, request: GoalUpdateRequest, userId: UUID): SavingGoal {
        val existingGoal = savingGoalRepository.findByIdAndUserId(goalId, userId)
            ?: throw IllegalArgumentException("Saving goal not found")

        val updatedGoal = existingGoal.copy(
            goalName = request.goalName ?: existingGoal.goalName,
            targetAmount = request.targetAmount ?: existingGoal.targetAmount,
            deadline = request.deadline ?: existingGoal.deadline,
            goalIcon = request.goalIcon ?: existingGoal.goalIcon,
            priority = request.priority ?: existingGoal.priority
        )

        return savingGoalRepository.save(updatedGoal)
    }

    @Transactional
    fun deleteGoal(userId: UUID, goalId: UUID) {
        if (savingGoalRepository.deleteByIdAndUserId(goalId, userId) == 0) {
            throw IllegalArgumentException("Saving goal not found")
        }
    }

    @Transactional(readOnly = true)
    fun getSavingGoalById(goalId: UUID, userId: UUID): GoalResponse {
        val goal = savingGoalRepository.findByIdAndUserId(goalId, userId)
            ?: throw IllegalArgumentException("Saving goal not found")
        val currentAmount = savingGoalHistoryRepository.getCurrentAmountByGoalId(goal.id)
        return goal.toResponse(currentAmount)
    }

    @Transactional(readOnly = true)
    fun getAllGoals(userId: UUID, pageable: Pageable): Page<GoalResponse> {
        val goalsPage = savingGoalRepository.findAllByUserId(userId, pageable)
        return goalsPage.map (GoalParams::toResponse)
    }

    @Transactional
    fun assignAmountToGoal(
        goalId: UUID,
        userId: UUID,
        request: AssignAmountRequest
    ) {
        val available = savingBalanceRepository.getUnassignedAmount(userId)

        if (available < request.amount) {
            throw IllegalArgumentException(" Insufficient unassigned amount to assign to the goal")
        }

        val goal = savingGoalRepository.findByIdAndUserId(goalId, userId)
            ?: throw IllegalArgumentException("Saving goal not found")

        savingGoalHistoryRepository.save(
            SavingGoalHistory(
                goal = goal,
                amount = request.amount
            )
        )
        val currentAmount = savingGoalHistoryRepository.getCurrentAmountByGoalId(goalId)
        if (currentAmount >= goal.targetAmount && !goal.isCompleted) {
            savingGoalRepository.save(goal.copy(isCompleted = true))
        }

        savingBalanceRepository.save(
            SavingBalance(
                userId = userId,
                unassignedAmount = request.amount.negate(),
            )
        )
    }

    @Transactional
    fun addToSavings(
        userId: UUID,
        amount: BigDecimal
    ) {
        savingBalanceRepository.save(
            SavingBalance(
                userId = userId,
                unassignedAmount = amount
            )
        )
    }

    fun getSummary(userId: UUID): GoalsSummary {

        return GoalsSummary(
            totalSaved = savingGoalRepository.sumSavedAmountByUserId(userId) ?: BigDecimal.ZERO,
            totalTarget = savingGoalRepository.sumTargetAmountByUserId(userId) ?: BigDecimal.ZERO,
            unassignedAmount = savingBalanceRepository.getUnassignedAmount(userId)
        )
    }


}