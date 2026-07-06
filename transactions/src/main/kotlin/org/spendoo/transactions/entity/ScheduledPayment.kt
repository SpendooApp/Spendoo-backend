package org.spendoo.transactions.entity

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.Instant
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
    val title: String,

    @Column(nullable = false)
    val amount: BigDecimal,

    @Column(nullable = false)
    val startDate: Instant,

    @Column(nullable = false)
    val nextDueDate: Instant,

    @Column(nullable = false)
    val nextReminderDate: Instant,

    @Column(nullable = false)
    val isNotified: Boolean = false,

    @Column(nullable = false)
    val frequency: Int,

    @Column(nullable = false)
    val reminderPeriod: Int,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val reminderUnit: ReminderUnit,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false)
    val category: Category

    )