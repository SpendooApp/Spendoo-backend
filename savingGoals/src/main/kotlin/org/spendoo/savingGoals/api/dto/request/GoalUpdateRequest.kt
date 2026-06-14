package org.spendoo.savingGoals.api.dto.request

import jakarta.validation.constraints.Future
import jakarta.validation.constraints.Positive
import org.spendoo.savingGoals.entity.GoalIcon
import java.math.BigDecimal
import java.time.LocalDateTime

data class GoalUpdateRequest(
    val goalName: String?,

    @field:Positive(message = "Target amount must be greater than 0")
    val targetAmount: BigDecimal?,

    @field:Future(message = "Deadline must be a future date")
    val deadline: LocalDateTime?,

    val goalIcon: GoalIcon?,

    val priority: Int?
)
