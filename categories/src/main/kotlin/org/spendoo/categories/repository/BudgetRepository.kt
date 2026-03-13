package org.spendoo.categories.repository

import org.spendoo.categories.entity.Budget
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.util.*

@Repository
interface BudgetRepository : JpaRepository<Budget, UUID> {
    fun findByCategoryCategoryId(categoryId: UUID): Budget?
    fun findByEndDateBefore(date: LocalDate): List<Budget>
}