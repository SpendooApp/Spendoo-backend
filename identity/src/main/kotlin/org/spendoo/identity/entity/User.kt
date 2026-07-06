package org.spendoo.identity.entity

import jakarta.persistence.*
import java.time.LocalDate
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "users", schema = "identity" )
data class User(
    @Id
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    val id: UUID = UUID.randomUUID(),

    @Column(nullable = false)
    val fullName: String,

    @Column(nullable = false, unique = true)
    val email: String,

    @Column(nullable = false)
    val passwordHash: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val gender: Gender,

    @Column(nullable = false)
    val birthDate: LocalDate,

    @Column(nullable = false)
    val isVerified: Boolean = false,

    @Column(nullable = false, updatable = false)
    val createdAt: Instant,

    @Column(nullable = true)
    val imageUrl: String? = null,

    @OneToMany(mappedBy = "user", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    val emailVerifications: List<EmailVerification> = emptyList(),

    @OneToMany(mappedBy = "user", cascade = [CascadeType.ALL], orphanRemoval = true)
    val refreshTokens: List<RefreshToken> = emptyList()
)
