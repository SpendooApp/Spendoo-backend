package org.spendoo.transactions.repository

import org.spendoo.transactions.api.dto.response.BudgetIntervalDto
import org.spendoo.transactions.entity.Budget
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime
import java.util.*

interface BudgetRepository : JpaRepository<Budget, UUID> {
    fun findByCategoryIdAndIsActiveIsTrue(categoryId: UUID): Budget?

    fun findByEndDateBeforeAndIsActiveIsTrueOrderByEndDateAscIdAsc(
        endDate: LocalDateTime,
        pageable: Pageable
    ): List<Budget>

    @Query(
        """
            SELECT new org.spendoo.transactions.api.dto.response.BudgetIntervalDto(b.amount, b.startDate, b.endDate)
            FROM Budget b
            WHERE b.category.userId = :userId
              AND b.category.isDeleted = false
              AND b.startDate <= :endDate
              AND b.endDate >= :startDate
              AND b.isActive = true
        """
    )
    fun findAllBudgetsByUserIdAndDateRange(
        @Param("userId") userId: UUID,
        @Param("startDate") startDate: LocalDateTime,
        @Param("endDate") endDate: LocalDateTime
    ): List<BudgetIntervalDto>
}