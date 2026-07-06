package org.spendoo.transactions.entity

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.Instant
import java.util.*

@Entity
@Table(name = "transactions", schema = "spending")
data class Transaction(

    @Id
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    val id: UUID = UUID.randomUUID(),

    @Column(columnDefinition = "uuid", nullable = false)
    val userId: UUID = UUID.randomUUID(),

    @Column(nullable = false)
    val title: String,

    @Column(nullable = false)
    val amount: BigDecimal,

    @Column(length = 500)
    val note: String?,

    @Column(nullable = false)
    val transactionDate: Instant,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = true)
    val category: Category?
)
