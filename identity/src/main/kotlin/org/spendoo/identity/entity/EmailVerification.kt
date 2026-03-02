package org.spendoo.identity.entity

import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "email_verification", schema = "identity")
data class EmailVerification(
    @Id
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    val id: UUID = UUID.randomUUID(),

    @Column(nullable = false)
    val otp: String,

    @Column(nullable = false)
    val sentAt: LocalDateTime = LocalDateTime.now(),

    @Column(nullable = false)
    val isUsed: Boolean,


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false)
    val user: User
) {
    fun isExpired(): Boolean {
        return sentAt.plusMinutes(15).isBefore(LocalDateTime.now())
    }
}