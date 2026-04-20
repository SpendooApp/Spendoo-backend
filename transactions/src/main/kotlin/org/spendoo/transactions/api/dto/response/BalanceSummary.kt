package org.spendoo.transactions.api.dto.response

import java.math.BigDecimal

/**
 * Represents a summary of a user's financial balance, providing an overview of their total balance,
 * income, and expenses.
 *
 * @property totalBalance The total balance, calculated as the sum of income and expenses.
 * @property income The total income, which includes active budgets and the sum of transactions without a specific category.
 * @property expenses The total expenses, calculated as the sum of negative transactions.
 */
data class BalanceSummary(
    val totalBalance: BigDecimal,
    val income: BigDecimal,
    val expenses: BigDecimal
)