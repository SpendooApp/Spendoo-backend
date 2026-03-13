package org.spendoo.categories.entity

import jakarta.persistence.*
import java.time.LocalDate
import java.util.*

@Entity
@Table(name = "budgets", schema = "categories")

data class Budget(
    @Id
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    val budgetId: UUID = UUID.randomUUID(),

    @Column(nullable = false)
    val amount: Double,

    @Column(nullable = false)
    val carryOver: Double = 0.0,

    @Column(nullable = false)
    val period: Int,

    @Column(nullable = false)
    val startDate: LocalDate,

    @Column(nullable = false)
    val endDate: LocalDate,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false)
    val category: Category
)
