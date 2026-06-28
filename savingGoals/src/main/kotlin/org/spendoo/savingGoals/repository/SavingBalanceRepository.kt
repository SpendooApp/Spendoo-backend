package org.spendoo.savingGoals.repository

import org.spendoo.savingGoals.entity.SavingBalance
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.math.BigDecimal
import java.util.*


interface SavingBalanceRepository : JpaRepository<SavingBalance, UUID> {

    @Query(
        """
    SELECT COALESCE(SUM(sb.unassignedAmount), 0)
    FROM SavingBalance sb
    WHERE sb.userId = :userId
"""
    )
    fun getUnassignedAmount(userId: UUID): BigDecimal
    fun existsByUserId(userId: UUID): Boolean

}