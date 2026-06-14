package org.spendoo.savingGoals.api.dto.response

import org.spendoo.savingGoals.entity.GoalIcon
import org.spendoo.savingGoals.entity.SavingGoal
import org.spendoo.savingGoals.service.model.GoalParams
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.*

data class GoalResponse(
    val goalId: UUID,

    val goalName: String,

    val priority: Int,

    val goalIcon: GoalIcon,

    val deadline: LocalDateTime,

    val currentAmount: BigDecimal,

    val targetAmount: BigDecimal,

    val savingPercentage: Int,

    val isCompleted: Boolean
)

fun SavingGoal.toResponse(currentAmount: BigDecimal): GoalResponse {

    val savingPercentage =
        if (this.targetAmount > BigDecimal.ZERO) ((currentAmount / this.targetAmount) * BigDecimal(100)).toInt() else 0

    return GoalResponse(
        goalId = this.id,
        goalName = this.goalName,
        priority = this.priority,
        goalIcon = this.goalIcon,
        deadline = this.deadline,
        currentAmount = currentAmount,
        targetAmount = this.targetAmount,
        isCompleted = this.isCompleted,
        savingPercentage = savingPercentage
    )
}

fun GoalParams.toResponse(): GoalResponse {
    val currentAmount = this.currentAmount ?: BigDecimal.ZERO
    val savingPercentage =
        if (this.targetAmount > BigDecimal.ZERO) ((currentAmount / this.targetAmount) * BigDecimal(100)).toInt() else 0

    return GoalResponse(
        goalId = this.goalId,
        goalName = this.goalName,
        priority = this.priority,
        goalIcon = this.goalIcon,
        deadline = this.deadline,
        currentAmount = currentAmount,
        targetAmount = this.targetAmount,
        isCompleted = this.isCompleted,
        savingPercentage = savingPercentage
    )
}

