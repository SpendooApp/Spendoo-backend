package org.spendoo.identity.entity

import jakarta.persistence.*
import jakarta.validation.constraints.Email
import java.time.LocalDateTime

@Entity
@Table(name = "users", schema = "identity" )
class User(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    val id: Long = 0,

    @Column(name = "full_name", nullable = false)
    var fullName: String = "",

    @Column(nullable = false, unique = true)
    @Email
    var email: String = "",

    @Column(name = "password_hash", nullable = false)
    var passwordHash: String = "",

    @Column(nullable = false)
    var gender: String = "",

    @Column(nullable = false)
    var age: Int = 0,

    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @OneToMany(mappedBy = "user", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    var emailVerifications: MutableList<EmailVerification> = mutableListOf()
)