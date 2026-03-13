package org.spendoo.categories.repository

import org.spendoo.categories.entity.Budget
import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface BudgetRepository : JpaRepository<Budget, UUID> {
    fun findByCategoryIdAndIsActiveIsTrue(categoryId: UUID): Budget?
}