package org.spendoo.transactions.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDate
import java.util.UUID

@Entity
@Table(name = "user_AI_usage", schema = "spending")
data class UserAIUsage(
    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(name = "user_id", nullable = false, unique = true)
    val userId: UUID,

    @Column(name = "ocr_count", nullable = false)
    val ocrCount: Int = 0,

    @Column(name = "voice_count", nullable = false)
    val voiceCount: Int = 0,

    @Column(name = "reset_date", nullable = false)
    val resetDate: LocalDate = LocalDate.now().plusMonths(1)

)
