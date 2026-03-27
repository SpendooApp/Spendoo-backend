package org.spendoo.transactions.entity

import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val type: TransactionType,

    @Column(nullable = false)
    val transactionDate: LocalDateTime,

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    val createdAt: LocalDateTime? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false)
    val category: Category
)

