package org.spendoo.identity.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "refresh_token", schema = "identity")
data class RefreshToken(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false, unique = true)
    val token: String,

    @Column(nullable = false)
    val expiryDate: LocalDateTime,

    @Column(nullable = true)
    val deviceToken: String? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: User
)