package org.spendoo.identity.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "email_verification", schema = "identity")
class EmailVerification(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "verification_id")
    val id: Long = 0,

    @Column(name = "verification_code", nullable = false)
    var verificationCode: String = "",

    @Column(nullable = false)
    var email: String = "",

    @Column(name = "sent_at", nullable = false)
    val sentAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "is_used", nullable = false)
    var isUsed: Boolean = false,


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    var user: User = User()
) {
    fun isExpired(): Boolean {
        return sentAt.plusMinutes(1).isBefore(LocalDateTime.now())
    }
}