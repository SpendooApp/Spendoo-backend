package org.spendoo.identity.entity

import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "email_verification", schema = "identity")
data class EmailVerification(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    val id: UUID? = null,

    @Column(nullable = false)
    val verificationCode: String,

    @Column(nullable = false)
    val email: String,

    @Column(nullable = false)
    val sentAt: LocalDateTime,

    @Column(nullable = false)
    val isUsed: Boolean,


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false)
    val user: User
) {
    fun isExpired(): Boolean {
        return sentAt.plusMinutes(1).isBefore(LocalDateTime.now())
    }
}