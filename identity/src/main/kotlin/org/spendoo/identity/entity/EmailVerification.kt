package org.spendoo.identity.entity

import jakarta.persistence.*
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*

@Entity
@Table(name = "email_verification", schema = "identity")
data class EmailVerification(
    @Id
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    val id: UUID = UUID.randomUUID(),

    @Column(nullable = false)
    val otp: String,

    @Column(nullable = false)
    val sentAt: Instant = Instant.now(),


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false)
    val user: User
) {
    fun isExpired(): Boolean {
        return sentAt.plus(15, ChronoUnit.MINUTES).isBefore(Instant.now())
    }
}