package org.spendoo.transactions.repository

import org.spendoo.transactions.entity.Category
import org.spendoo.transactions.service.model.CategoriesSummary
import org.spendoo.transactions.service.model.CategoryParams
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.math.BigDecimal
import java.util.*

interface CategoryRepository : JpaRepository<Category, UUID> {

    @Query(
        """
            SELECT NEW org.spendoo.transactions.service.model.CategoryParams(
                c.id,
                c.categoryName,
                c.categoryIcon,
                c.priority,
                c.leftOverOptions,
                b.id,
                b.amount,
                b.carryOver,
                b.period,
                b.startDate,
                b.endDate,
                SUM(t.amount)
            )
            FROM Category c
            JOIN c.budgets b
                ON b.isActive = true
            LEFT JOIN c.transactions t
                ON t.amount < 0
                AND t.transactionDate BETWEEN b.startDate AND b.endDate
            WHERE c.userId = :userId
                AND c.isDeleted = false
            GROUP BY
                c.id,
                c.categoryName,
                c.categoryIcon,
                c.priority,
                c.leftOverOptions,
                b.id,
                b.amount,
                b.carryOver,
                b.period,
                b.startDate,
                b.endDate
        """
    )
    fun getExistCategoriesWithBudgetAndSpending(userId: UUID, pageable: Pageable): Page<CategoryParams>

    @Query(
        """
            SELECT NEW org.spendoo.transactions.service.model.CategoryParams(
                c.id,
                c.categoryName,
                c.categoryIcon,
                c.priority,
                c.leftOverOptions,
                b.id,
                b.amount,
                b.carryOver,
                b.period,
                b.startDate,
                b.endDate,
                Sum(t.amount)
            )
            FROM Category c
            JOIN c.budgets b
                ON b.isActive = true
            LEFT JOIN c.transactions t
            ON t.amount < 0
            AND t.transactionDate BETWEEN b.startDate AND b.endDate
            WHERE
                c.id = :categoryId
                AND c.userId = :userId
                AND c.isDeleted = false
            GROUP BY
                c.id,
                c.categoryName,
                c.categoryIcon,
                c.priority,
                c.leftOverOptions,
                b.id,
                b.amount,
                b.carryOver,
                b.period,
                b.startDate,
                b.endDate
        """
    )
    fun getExistCategoryWithBudgetAndSpending(categoryId: UUID, userId: UUID): CategoryParams?

    fun findByIdAndUserIdAndIsDeletedFalse(id: UUID, userId: UUID): Category?

    @Query(
        """
            SELECT SUM(b.amount)
            FROM Category c
            JOIN c.budgets b
            WHERE c.userId = :userId
                AND c.isDeleted = false
                AND b.isActive = true
        """
    )
    fun sumActiveBudget(userId: UUID): BigDecimal?

    @Query("""
    SELECT NEW org.spendoo.transactions.service.model.CategoriesSummary(
    
        (SELECT COALESCE(SUM(b.amount), 0.0)
         FROM Budget b
         WHERE b.isActive = true
           AND b.category.userId = :userId
           AND b.category.isDeleted = false),
    
        (SELECT SUM(t.amount)
         From Category c 
         Join c.transactions t
         JOIN c.budgets b
            ON b.isActive = true
         WHERE c.userId = :userId
           AND t.amount < 0
           AND c.isDeleted = false
           AND t.transactionDate BETWEEN b.startDate AND b.endDate
        ),
    
        (SELECT SUM(i.amount)
         FROM Transaction i
         WHERE i.userId = :userId
           AND i.category IS NULL)
    )
    """)
    fun getCategoriesSummaryForUser(userId: UUID): CategoriesSummary
}