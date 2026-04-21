package org.spendoo.transactions.service.model

import java.math.BigDecimal

/**
 * Represents a summary of the user's category-related financial data, including total budgets,
 * total spending, and additional income that can be allocated to any category.
 *
 * @property totalBudget The sum of all active budgets for the user's categories.
 * @property totalSpent The sum of all expenses (negative transactions) for the user's categories
 *                      within the active budget periods, or across all transactions if no active budget exists.
 * @property addedIncome The total of all transactions without a category for the user,
 *                       indicating additional allocable funds.
 */
data class CategoriesSummary(
    val totalBudget: BigDecimal?,
    val totalSpent: BigDecimal?,
    val addedIncome: BigDecimal?
)