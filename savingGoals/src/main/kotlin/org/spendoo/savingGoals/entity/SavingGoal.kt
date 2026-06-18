package org.spendoo.savingGoals.entity

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.*

@Entity
@Table(name = "saving_goals", schema = "saving_goals")
data class SavingGoal(

    @Id
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    val id: UUID = UUID.randomUUID(),

    @Column(columnDefinition = "uuid", nullable = false)
    val userId: UUID = UUID.randomUUID(),

    @Column(nullable = false)
    val goalName: String,

    @Column(nullable = false)
    val priority: Int,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val goalIcon: GoalIcon,

    @Column(nullable = false)
    val deadline: LocalDateTime,

    @Column(nullable = false)
    val targetAmount: BigDecimal,

    @Column(nullable = false)
    val isCompleted: Boolean = false,

    @OneToMany(mappedBy = "goal", cascade = [CascadeType.ALL], fetch = FetchType.LAZY, orphanRemoval = true)
    val savingGoalHistory: MutableList<SavingGoalHistory> = mutableListOf(),
)
