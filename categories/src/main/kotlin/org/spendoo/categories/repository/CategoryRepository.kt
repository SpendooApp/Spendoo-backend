package org.spendoo.categories.repository

import org.spendoo.categories.entity.Category
import org.spendoo.categories.service.model.CategoryParams
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.*

interface CategoryRepository : JpaRepository<Category, UUID>
{
    @Query(
        """
            SELECT NEW org.spendoo.categories.service.model.CategoryParams(
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
            )
            FROM Category c
            LEFT JOIN Budget b
                ON c.id = b.category.id
            WHERE c.userId = :userId
                AND b.isActive = true
                AND c.isDeleted = false
        """
    )
    fun getExistCategoriesWithBudget(userId: UUID, pageable: Pageable): Page<CategoryParams>

    @Query(
        """
            SELECT NEW org.spendoo.categories.service.model.CategoryParams(
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
            )
            FROM Category c
            LEFT JOIN Budget b
                ON c.id = b.category.id
            WHERE
                c.id = :categoryId
                AND c.userId = :userId
                AND b.isActive = true
                AND c.isDeleted = false
        """
    )
    fun getExistCategoryWithBudget(categoryId: UUID, userId: UUID): CategoryParams?

    fun findByIdAndUserIdAndIsDeletedFalse(id: UUID, userId: UUID): Category?
}