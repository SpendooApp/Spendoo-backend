package org.spendoo.categories.service

import org.spendoo.categories.api.dto.request.BudgetCreateRequest
import org.spendoo.categories.api.dto.request.toBudget
import org.spendoo.categories.entity.Budget
import org.spendoo.categories.entity.Category
import org.spendoo.categories.entity.LeftOverOptions
import org.spendoo.categories.repository.BudgetRepository
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.*

@Service
class BudgetService(private val budgetRepository: BudgetRepository) {

    fun createBudget(
        request: BudgetCreateRequest,
        category: Category,
        carryOver: BigDecimal = BigDecimal.ZERO
    ): Budget {
        val alignedStartDate = alignStartDateToActiveCycle(request.startDate, request.period)
        return budgetRepository.save(request.copy(startDate = alignedStartDate).toBudget(category, carryOver))
    }

    fun updateBudget(request: BudgetCreateRequest, category: Category): Budget {
        var carryOver = processExpiredBudget(category.id)
        if (carryOver < BigDecimal.ZERO) carryOver = BigDecimal.ZERO
        return createBudget(request, category, carryOver)
    }

    fun processExpiredBudget(categoryId: UUID): BigDecimal {
        budgetRepository.findByCategoryIdAndIsActiveIsTrue(categoryId)?.let { expiredBudget ->

            val category = expiredBudget.category

            val spentAmount = calculateSpentAmount(category.id)

            val leftover = expiredBudget.amount - spentAmount.toBigDecimal()

            budgetRepository.save(
                expiredBudget.copy(isActive = false)
            )

            return when (category.leftOverOptions) {
                LeftOverOptions.MOVE_TO_NEXT_PERIOD -> {
                    leftover
                }

                LeftOverOptions.RESET_TO_ORIGINAL_AMOUNT -> {
                    BigDecimal.ZERO
                }

                LeftOverOptions.MOVE_TO_SAVINGS -> {
                    moveToSavings(leftover)
                    leftover
                }
            }
        }
        return BigDecimal.ZERO
    }

    fun moveToSavings(amount: BigDecimal) {
        // call savings service
    }

    fun calculateSpentAmount(categoryId: UUID): Double {
        // call transaction service to get total spent amount for the category
        return 0.0
    }

    private fun alignStartDateToActiveCycle(
        startDate: LocalDateTime,
        periodDays: Int,
        now: LocalDateTime = LocalDateTime.now()
    ): LocalDateTime {
        val period = periodDays.toLong()
        val initialEndDate = startDate.plusDays(period)

        if (initialEndDate.isAfter(now)) return startDate

        val overdueDays = ChronoUnit.DAYS.between(initialEndDate, now)
        val cyclesToShift = (overdueDays / period) + 1

        return startDate.plusDays(cyclesToShift * period)
    }
}