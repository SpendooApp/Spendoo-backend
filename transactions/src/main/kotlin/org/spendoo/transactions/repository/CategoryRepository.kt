package org.spendoo.transactions.repository

import org.spendoo.transactions.entity.Category
import org.spendoo.transactions.service.model.CategoryParams
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
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
                Sum(
                CASE WHEN t.amount < 0
                    And (
                            b is null
                            Or t.transactionDate BETWEEN b.startDate AND b.endDate
                        )
                    THEN t.amount 
                    ELSE 0 
                END
                )
            )
            FROM Category c
            LEFT JOIN c.budgets b
                ON b.isActive = true
            LEFT JOIN c.transactions t
            WHERE c.userId = :userId
                AND c.isDeleted = false
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
                Sum(
                CASE WHEN t.amount < 0
                    And (
                            b is null
                            Or t.transactionDate BETWEEN b.startDate AND b.endDate
                        )
                    THEN t.amount 
                    ELSE 0 
                END
                )
            )
            FROM Category c
            LEFT JOIN c.budgets b
                ON b.isActive = true
            LEFT JOIN c.transactions t
            WHERE
                c.id = :categoryId
                AND c.userId = :userId
                AND c.isDeleted = false
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

    @Modifying
    @Query(
        """
            INSERT INTO spending.categories
            (id, category_name, category_icon, priority, left_over_options, user_id, is_deleted)
            VALUES (gen_random_uuid(), 'Food', 'FOOD', 2, 'RESET_TO_ORIGINAL_AMOUNT', :userId, false),
                   (gen_random_uuid(), 'Transport', 'TRANSPORT', 2, 'RESET_TO_ORIGINAL_AMOUNT', :userId, false),
                   (gen_random_uuid(), 'Shopping', 'SHOPPING', 2, 'RESET_TO_ORIGINAL_AMOUNT', :userId, false),
                   (gen_random_uuid(), 'HealthCare', 'HEALTHCARE', 3, 'RESET_TO_ORIGINAL_AMOUNT', :userId, false),
                   (gen_random_uuid(), 'Entertainment', 'ENTERTAINMENT', 2, 'RESET_TO_ORIGINAL_AMOUNT', :userId, false),
                   (gen_random_uuid(), 'Other', 'DEFAULT', 1, 'RESET_TO_ORIGINAL_AMOUNT', :userId, false);
        """,
        nativeQuery = true
    )
    fun insertDefaultCategoriesForUser(userId: UUID)
}