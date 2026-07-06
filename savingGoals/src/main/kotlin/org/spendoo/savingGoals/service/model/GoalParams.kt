package org.spendoo.savingGoals.service.model

import org.spendoo.savingGoals.entity.GoalIcon
import java.math.BigDecimal
import java.time.Instant
import java.util.*

class GoalParams(
    val goalId: UUID,

    val goalName: String,

    val priority: Int,

    val goalIcon: GoalIcon,

    val deadline: Instant,

    val currentAmount: BigDecimal?,

    val targetAmount: BigDecimal,

    val isCompleted: Boolean,

    )