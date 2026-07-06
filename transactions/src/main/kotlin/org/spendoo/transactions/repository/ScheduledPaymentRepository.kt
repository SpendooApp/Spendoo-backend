package org.spendoo.transactions.repository

import org.spendoo.transactions.entity.ScheduledPayment
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

interface ScheduledPaymentRepository : JpaRepository<ScheduledPayment, UUID> {
    fun findAllByUserId(userId: UUID, pageable: Pageable): Page<ScheduledPayment>

    @Query("SELECT SUM(p.amount) FROM ScheduledPayment p WHERE p.userId = :userId")
    fun sumAmountByUserId(@Param("userId") userId: UUID): BigDecimal?

    @Query("SELECT COUNT(sp) FROM ScheduledPayment sp WHERE sp.userId = :userId AND sp.nextDueDate > :now")
    fun countUpcomingByUserId(@Param("userId") userId: UUID, @Param("now") now: Instant): Long

    fun findByNextReminderDateBeforeAndIsNotifiedFalse(now: Instant, pageable: Pageable): Page<ScheduledPayment>

    fun findByNextDueDateBefore(now: Instant, pageable: Pageable): Page<ScheduledPayment>

    @Modifying
    @Query("UPDATE ScheduledPayment sp SET sp.isNotified = true WHERE sp.id IN :ids")
    fun markPaymentsNotified(@Param("ids") ids: List<UUID>)
}