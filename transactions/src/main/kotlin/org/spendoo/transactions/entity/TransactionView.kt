package org.spendoo.transactions.entity

import jakarta.persistence.*
import org.hibernate.annotations.Immutable
import org.hibernate.annotations.Subselect
import org.hibernate.annotations.Synchronize
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.*

@Entity
@Subselect("""
    SELECT 
        id, user_id as user_id, title, amount, note, transaction_date as transaction_date, category_id as category_id, 
        CASE WHEN amount >= 0 THEN 'INCOME' ELSE 'EXPENSE' END as type
    FROM spending.transactions
    UNION ALL
    SELECT 
        b.id, c.user_id as user_id, c.category_name as title, b.amount, null as note, b.start_date as transaction_date, b.category_id as category_id, 
        'BUDGET' as type
    FROM spending.budgets b
    JOIN spending.categories c ON b.category_id = c.id
    WHERE b.amount <> 0
""")
@Synchronize("spending.transactions", "spending.budgets", "spending.categories")
@Immutable
data class TransactionView(

    @Id
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    val id: UUID,

    @Column(columnDefinition = "uuid", nullable = false, name = "user_id")
    val userId: UUID,

    @Column(nullable = false)
    val title: String,

    @Column(nullable = false)
    val amount: BigDecimal,

    @Column(length = 500)
    val note: String?,

    @Column(nullable = false, name = "transaction_date")
    val transactionDate: LocalDateTime,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    val category: Category?,

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    val type: TransactionType
)
