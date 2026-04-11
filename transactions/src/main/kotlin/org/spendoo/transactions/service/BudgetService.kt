package org.spendoo.transactions.service

import org.spendoo.transactions.api.dto.request.BudgetCreateRequest
import org.spendoo.transactions.api.dto.request.toBudget
import org.spendoo.transactions.entity.Budget
import org.spendoo.transactions.entity.Category
import org.spendoo.transactions.entity.LeftOverOptions
import org.spendoo.transactions.repository.BudgetRepository
import org.spendoo.transactions.repository.TransactionRepository
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.*

@Service
class BudgetService(
    private val budgetRepository: BudgetRepository,
    private val transactionRepository: TransactionRepository,
) {

    fun createBudget(
        request: BudgetCreateRequest,
        category: Category,
        carryOver: BigDecimal = BigDecimal.ZERO
    ): Budget {
        val alignedStartDate = alignStartDateToActiveCycle(request.startDate, request.period)
        return budgetRepository.save(request.copy(startDate = alignedStartDate).toBudget(category, carryOver))
    }

    fun updateBudget(request: BudgetCreateRequest, category: Category): Budget {
        val activeBudget = budgetRepository.findByCategoryIdAndIsActiveIsTrue(category.id)
        activeBudget ?: return createBudget(request, category)

        val alignedStartDate = alignStartDateToActiveCycle(request.startDate, request.period)
        val updatedBudget = activeBudget.copy(
            amount = request.amount.toBigDecimal(),
            carryOver = BigDecimal.ZERO,
            period = request.period,
            startDate = alignedStartDate,
            endDate = alignedStartDate.plusDays(request.period.toLong()),
            isActive = true
        )

        return budgetRepository.save(updatedBudget)
    }

    fun processExpiredBudget(categoryId: UUID): BigDecimal {
        budgetRepository.findByCategoryIdAndIsActiveIsTrue(categoryId)?.let { expiredBudget ->

            val category = expiredBudget.category

            val spentAmount = calculateSpentAmount(category.userId, category.id)

            val leftover = expiredBudget.amount - spentAmount

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

    fun calculateSpentAmount(userId: UUID, categoryId: UUID): BigDecimal {
        return transactionRepository.sumAmountByUserIdAndCategoryId(userId, categoryId)
            ?: BigDecimal.ZERO
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

    fun deactivateBudgetForCategory(categoryId: UUID) {
        budgetRepository.findByCategoryIdAndIsActiveIsTrue(categoryId)?.let { expiredBudget ->
            budgetRepository.save(expiredBudget.copy(isActive = false))
        }
    }
}
