package org.spendoo.savingGoals.entity

import jakarta.persistence.*
import java.math.BigDecimal
import java.util.*

@Entity
@Table(name = "user_achievements", schema = "saving_goals")
data class UserAchievement(
    @Id
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    val id: UUID = UUID.randomUUID(),

    @Column(columnDefinition = "uuid", nullable = false)
    val userId: UUID = UUID.randomUUID(),

    @Column(nullable = false)
    var currentProgress: BigDecimal = BigDecimal.ZERO,

    @Column(nullable = false)
    var isUnlocked: Boolean = false,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false)
    val achievement: Achievement
)
