package org.spendoo.savingGoals.api.dto.response

import java.math.BigDecimal

/**
 * Represents a summary of a user's saving goals and savings allocation.
 *
 * @property totalSaved The total amount currently saved across all saving goals.
 * @property totalTarget The total target amount required to achieve all saving goals.
 * @property unassignedAmount The amount available in savings that has not yet been assigned
 * to any specific saving goal. This amount may come from carry-over budgets or manually allocated income.
 */
data class GoalsSummary(
    val totalSaved: BigDecimal,
    val totalTarget: BigDecimal,
    val unassignedAmount: BigDecimal
)
