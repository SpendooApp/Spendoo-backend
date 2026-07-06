package org.spendoo.transactions.repository

import org.spendoo.transactions.entity.Budget
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import java.time.Instant
import java.util.*

interface BudgetRepository : JpaRepository<Budget, UUID> {
    fun findByCategoryIdAndIsActiveIsTrue(categoryId: UUID): Budget?

    fun findByEndDateBeforeAndIsActiveIsTrueOrderByEndDateAscIdAsc(
        endDate: Instant,
        pageable: Pageable
    ): List<Budget>

}