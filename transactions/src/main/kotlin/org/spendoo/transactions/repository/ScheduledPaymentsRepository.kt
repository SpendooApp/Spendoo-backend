package org.spendoo.transactions.repository

import org.spendoo.transactions.entity.ScheduledPayment
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.util.UUID

@Repository
interface ScheduledPaymentRepository : JpaRepository<ScheduledPayment, UUID> {
    fun findAllByUserId(userId: UUID, pageable: Pageable): Page<ScheduledPayment>

    @Query("SELECT SUM(p.amount) FROM ScheduledPayment p WHERE p.userId = :userId")
    fun sumAmountByUserId(userId: UUID): BigDecimal?
}