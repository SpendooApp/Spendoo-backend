package org.spendoo.transactions.entity

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "scheduled_payments", schema = "spending")
data class ScheduledPayment(
    @Id
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    val id: UUID = UUID.randomUUID(),

    @Column(nullable = false)
    val userId: UUID = UUID.randomUUID(),

    @Column(nullable = false)
    val categoryId: UUID = UUID.randomUUID(),

    @Column(nullable = false)
    val title: String,

    @Column(nullable = false)
    val amount: BigDecimal,

    @Column(nullable = false)
    val startDate: LocalDateTime,

    @Column(nullable = false)
    val nextDueDate: LocalDateTime,

    @Column(nullable = false)
    val nextReminderDate: LocalDateTime,

    @Column(nullable = false)
    val frequency: Int,

    @Column(nullable = false)
    val reminderPeriod: Int,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val reminderUnit: ReminderUnit,

    )