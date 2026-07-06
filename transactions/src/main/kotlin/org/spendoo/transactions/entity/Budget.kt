package org.spendoo.transactions.entity

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.Instant
import java.util.*

@Entity
@Table(name = "budgets", schema = "spending")

data class Budget(
    @Id
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    val id: UUID = UUID.randomUUID(),

    @Column(nullable = false)
    val amount: BigDecimal,

    @Column(nullable = false)
    val carryOver: BigDecimal = BigDecimal.ZERO,

    @Column(nullable = false)
    val period: Int,

    @Column(nullable = false)
    val startDate: Instant,

    @Column(nullable = false)
    val endDate: Instant,

    @Column(nullable = false)
    val isActive: Boolean,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false)
    val category: Category
)
