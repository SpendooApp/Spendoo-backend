package org.spendoo.categories.scheduler

import org.spendoo.categories.api.dto.request.BudgetCreateRequest
import org.spendoo.categories.repository.BudgetRepository
import org.spendoo.categories.service.BudgetService
import org.springframework.data.domain.PageRequest
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDateTime

@Component
class BudgetExpirationScheduler(
    private val budgetRepository: BudgetRepository,
    private val budgetService: BudgetService
) {

    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    fun runDailyBudgetRollOver() {
        val now = LocalDateTime.now()
        val batchRequest = PageRequest.of(0, BATCH_SIZE)

        while (true) {
            val expiredBudgets = budgetRepository
                .findByEndDateBeforeAndIsActiveIsTrueOrderByEndDateAscIdAsc(now, batchRequest)

            if (expiredBudgets.isEmpty()) break

            for (expired in expiredBudgets) {
                try {
                    val category = expired.category
                    var carryOver = budgetService.processExpiredBudget(category.id)
                    if (carryOver < BigDecimal.ZERO) carryOver = BigDecimal.ZERO

                    val baseAmount = expired.amount - expired.carryOver
                    val request = BudgetCreateRequest(
                        amount = baseAmount.toDouble(),
                        period = expired.period,
                        startDate = expired.endDate
                    )

                    budgetService.createBudget(request, category, carryOver)
                } catch (ex: Exception) {
                    println("Failed processing expired budget ${expired.id}: ${ex.message}")
                }
            }
        }
    }

    companion object {
        private const val BATCH_SIZE = 50
    }
}
