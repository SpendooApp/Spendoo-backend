package org.spendoo.categories.service

import org.spendoo.categories.api.dto.request.BudgetCreateRequest
import org.spendoo.categories.entity.Budget
import org.spendoo.categories.entity.Category
import org.spendoo.categories.entity.LeftOverOptions
import org.spendoo.categories.repository.BudgetRepository
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.util.UUID

@Service
class BudgetService (private val budgetRepository: BudgetRepository) {

    fun createBudget(request: BudgetCreateRequest, category: Category): Budget {
        val endDate = request.startDate.plusDays(request.period.toLong())
        val budget = Budget(
            category = category,
            amount = request.amount,
            startDate = request.startDate,
            endDate = endDate,
            period = request.period
        )
        return budgetRepository.save(budget)
    }

    fun updateBudget(request: BudgetCreateRequest, budget: Budget): Budget {

        val endDate = request.startDate.plusMonths(request.period.toLong())
        val updatedBudget = budget.copy(
            amount = request.amount,
            period = request.period,
            startDate = request.startDate,
            endDate = endDate
        )

        return budgetRepository.save(updatedBudget)
    }

    fun processExpiredBudget() {
        val expiredBudgets = budgetRepository.findByEndDateBefore(LocalDate.now())

        expiredBudgets.forEach { budget ->

            val category = budget.category

            val spentAmount = calculateSpentAmount(category.categoryId)

            val leftover = budget.amount - spentAmount

            when (category.leftOverOptions) {

                LeftOverOptions.MOVE_TO_NEXT_PERIOD -> {
                    createNextBudget(budget, leftover)
                }

                LeftOverOptions.RESET_TO_ORIGINAL_AMOUNT -> {
                    createNextBudget(budget, 0.0)
                }

                LeftOverOptions.MOVE_TO_SAVINGS -> {
                    moveToSavings(leftover)
                    createNextBudget(budget, 0.0)
                }
            }
        }
    }
    fun moveToSavings(amount: Double) {
        // call savings service
    }

    fun createNextBudget(budget: Budget, carryOver: Double) {

        val newStartDate = budget.endDate.plusDays(1)
        val newEndDate = newStartDate.plusDays(budget.period.toLong())

        val newBudget = Budget(
            amount = budget.amount + carryOver,
            carryOver = carryOver,
            period = budget.period,
            startDate = newStartDate,
            endDate = newEndDate,
            category = budget.category
        )

        budgetRepository.save(newBudget)
    }
    fun calculateSpentAmount(categoryId: UUID): Int {
        // call transaction service to get total spent amount for the category
        return 0
    }
}