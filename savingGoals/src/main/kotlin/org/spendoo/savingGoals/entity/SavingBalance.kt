package org.spendoo.savingGoals.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.math.BigDecimal
import java.util.*

@Entity
@Table(name = "saving_balance", schema = "saving_goals")
data class SavingBalance(
    @Id
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    val id: UUID = UUID.randomUUID(),

    @Column(columnDefinition = "uuid", nullable = false)
    val userId: UUID = UUID.randomUUID(),

    @Column(nullable = false)
    val unassignedAmount: BigDecimal = BigDecimal.ZERO,
)
