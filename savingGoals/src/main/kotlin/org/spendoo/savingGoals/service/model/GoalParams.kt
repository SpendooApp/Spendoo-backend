package org.spendoo.savingGoals.service.model

import org.spendoo.savingGoals.entity.GoalIcon
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

class GoalParams (
    val goalId: UUID,

    val goalName: String,

    val priority: Int,

    val goalIcon: GoalIcon,

    val deadline: LocalDateTime,

    val currentAmount: BigDecimal?,

    val targetAmount: BigDecimal,

    val isCompleted: Boolean,

)