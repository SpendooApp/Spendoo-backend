package org.spendoo.savingGoals.entity

import jakarta.persistence.*
import java.math.BigDecimal
import java.util.*

@Entity
@Table(name = "achievements", schema = "saving_goals")
data class Achievement(
    @Id
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    val id: UUID = UUID.randomUUID(),

    @Column(nullable = false)
    val targetValue: BigDecimal,

    @Column(nullable = false)
    val level: Int,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val achievementType: AchievementType,

    @Column(nullable = false)
    val titleEn: String,

    @Column(nullable = false)
    val titleAr: String,

    @Column(nullable = false)
    val descriptionEn: String,

    @Column(nullable = false)
    val descriptionAr: String
)
