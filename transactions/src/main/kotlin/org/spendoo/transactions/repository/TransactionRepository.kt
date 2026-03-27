package org.spendoo.transactions.repository

import org.spendoo.transactions.api.dto.response.CategorySpendingDto
import org.spendoo.transactions.entity.Transaction
import org.spendoo.transactions.entity.TransactionType
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.*

interface TransactionRepository : JpaRepository<Transaction, UUID> {

    fun findAllByUserId(userId: UUID): List<Transaction>

    fun findAllByUserIdAndTransactionDateBetween(
        userId: UUID,
        startDate: LocalDateTime,
        endDate: LocalDateTime
    ): List<Transaction>

    @Query("SELECT SUM(t.amount) FROM Transaction t WHERE t.userId = :userId AND t.type = :type")

    fun sumAmountByUserIdAndType(
        @Param("userId") userId: UUID,
        @Param("type") type: TransactionType
    ): BigDecimal?

    @Query(
        """
        SELECT new org.spendoo.transactions.api.dto.response.CategorySpendingDto(c.categoryName, c.categoryIcon, SUM(t.amount)) 
        FROM Transaction t 
        JOIN t.category c 
        WHERE t.userId = :userId AND t.type = 'EXPENSE' 
        GROUP BY c.id, c.categoryName, c.categoryIcon 
        ORDER BY SUM(t.amount) DESC
    """
    )
    fun findTopSpendingCategories(
        @Param("userId") userId: UUID,
        pageable: Pageable
    ): List<CategorySpendingDto>

    fun findByIdAndUserId(id: UUID, userId: UUID): Transaction?
}