package org.spendoo.savingGoals.api.dto.request

import jakarta.validation.constraints.Future
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive
import org.spendoo.savingGoals.entity.GoalIcon
import org.spendoo.savingGoals.entity.SavingGoal
import java.math.BigDecimal
import java.time.Instant
import java.util.*

data class GoalCreateRequest(
    @field:NotBlank(message = "Goal name must not be blank")
    val goalName: String,

    @field:Positive(message = "Target amount must be greater than 0")
    val targetAmount: BigDecimal,

    @field:Future(message = "Deadline must be a future date")
    val deadline: Instant,

    val goalIcon: GoalIcon,

    val priority: Int
)

fun GoalCreateRequest.toEntity(userId: UUID): SavingGoal {
    return SavingGoal(
        userId = userId,
        goalName = goalName,
        targetAmount = targetAmount,
        deadline = deadline,
        priority = priority,
        goalIcon = goalIcon
    )
}
