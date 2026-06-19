package org.spendoo.transactions.repository

import org.spendoo.transactions.api.dto.response.CategorySpendingDto
import org.spendoo.transactions.entity.Transaction
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.*

import org.springframework.data.jpa.repository.JpaSpecificationExecutor

interface TransactionRepository : JpaRepository<Transaction, UUID>, JpaSpecificationExecutor<Transaction> {

    fun findAllByUserId(userId: UUID, pageable: Pageable): Page<Transaction>

    fun findAllByUserIdAndTransactionDateBetween(
        userId: UUID,
        startDate: LocalDateTime,
        endDate: LocalDateTime,
        pageable: Pageable
    ): Page<Transaction>

    @Query("""SELECT SUM(t.amount) FROM Transaction t WHERE t.userId = :userId AND t.category IS NULL""")
    fun sumIncomeByUserId(@Param("userId") userId: UUID): BigDecimal?

    @Query("""SELECT SUM(t.amount) FROM Transaction t WHERE t.userId = :userId AND t.amount < 0 AND t.category IS NOT NULL""")
    fun sumExpensesByUserId(@Param("userId") userId: UUID): BigDecimal?

    @Query(
        """
        SELECT new org.spendoo.transactions.api.dto.response.CategorySpendingDto(c.id, c.categoryName, c.categoryIcon, SUM(t.amount)) 
        FROM Transaction t 
        JOIN t.category c 
        WHERE t.userId = :userId AND t.amount < 0
        GROUP BY c.id, c.categoryName, c.categoryIcon 
        ORDER BY ABS(SUM(t.amount)) DESC
    """
    )
    fun findTopSpendingCategories(
        @Param("userId") userId: UUID,
        pageable: Pageable
    ): Page<CategorySpendingDto>

    @Query(
        """
            SELECT SUM(t.amount)
            FROM Transaction t
            JOIN t.category c
            WHERE t.userId = :userId
                AND t.amount < 0
                AND c.id = :categoryId
        """
    )
    fun sumAmountByUserIdAndCategoryId(
        @Param("userId") userId: UUID,
        @Param("categoryId") categoryId: UUID
    ): BigDecimal?

    @Query(
        """
            SELECT COALESCE(SUM(t.amount), 0)
            FROM Transaction t
            WHERE t.userId = :userId
              AND t.amount < 0
              AND t.transactionDate >= :startDate
              AND t.transactionDate < :endDate
        """
    )
    fun sumExpensesByUserIdAndDateRange(
        @Param("userId") userId: UUID,
        @Param("startDate") startDate: LocalDateTime,
        @Param("endDate") endDate: LocalDateTime
    ): BigDecimal

    @Query(
        """
            SELECT COALESCE(SUM(t.amount), 0)
            FROM Transaction t
            WHERE t.userId = :userId
              AND t.amount >= 0
              AND t.transactionDate >= :startDate
              AND t.transactionDate < :endDate
        """
    )
    fun sumIncomeByUserIdAndDateRange(
        @Param("userId") userId: UUID,
        @Param("startDate") startDate: LocalDateTime,
        @Param("endDate") endDate: LocalDateTime
    ): BigDecimal

    @Query(
        """
            SELECT new org.spendoo.transactions.api.dto.response.CategorySpendingDto(c.categoryName, c.categoryIcon, SUM(t.amount))
            FROM Transaction t
            JOIN t.category c
            WHERE t.userId = :userId
              AND t.amount < 0
              AND t.transactionDate >= :startDate
              AND t.transactionDate < :endDate
            GROUP BY c.id, c.categoryName, c.categoryIcon
            ORDER BY ABS(SUM(t.amount)) DESC
        """
    )
    fun findTopSpendingCategoriesInDateRange(
        @Param("userId") userId: UUID,
        @Param("startDate") startDate: LocalDateTime,
        @Param("endDate") endDate: LocalDateTime
    ): List<CategorySpendingDto>

    @Query(
        """
            SELECT new org.spendoo.transactions.api.dto.response.CategorySpendingDto(c.categoryName, c.categoryIcon, SUM(t.amount))
            FROM Transaction t
            JOIN t.category c
            WHERE t.userId = :userId
              AND t.amount >= 0
              AND t.transactionDate >= :startDate
              AND t.transactionDate < :endDate
            GROUP BY c.id, c.categoryName, c.categoryIcon
            ORDER BY ABS(SUM(t.amount)) DESC
        """
    )
    fun findTopIncomeCategoriesInDateRange(
        @Param("userId") userId: UUID,
        @Param("startDate") startDate: LocalDateTime,
        @Param("endDate") endDate: LocalDateTime
    ): List<CategorySpendingDto>

    @Query(
        """
            SELECT new org.spendoo.transactions.api.dto.response.CategorySpendingDto(c.categoryName, c.categoryIcon, SUM(t.amount))
            FROM Transaction t
            JOIN t.category c
            WHERE t.userId = :userId
              AND t.transactionDate >= :startDate
              AND t.transactionDate < :endDate
            GROUP BY c.id, c.categoryName, c.categoryIcon
            ORDER BY ABS(SUM(t.amount)) DESC
        """
    )
    fun findTopAllCategoriesInDateRange(
        @Param("userId") userId: UUID,
        @Param("startDate") startDate: LocalDateTime,
        @Param("endDate") endDate: LocalDateTime
    ): List<CategorySpendingDto>

    fun findByIdAndUserId(id: UUID, userId: UUID): Transaction?

    fun deleteByIdAndUserId(id: UUID, userId: UUID): Int
}
