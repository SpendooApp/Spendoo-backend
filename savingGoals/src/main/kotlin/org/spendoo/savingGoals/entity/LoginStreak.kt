package org.spendoo.savingGoals.entity

import jakarta.persistence.*
import java.time.LocalDate
import java.util.UUID

@Entity
@Table(name = "login_streaks", schema = "saving_goals")
data class LoginStreak(
    @Id
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    val id: UUID = UUID.randomUUID(),

    @Column(columnDefinition = "uuid", nullable = false)
    val userId: UUID,

    @Column(nullable = false)
    val streakDate: LocalDate
)
