package org.spendoo.identity.entity

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(
    name = "follows", schema = "identity",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_follower_followee",
            columnNames = ["follower_id", "followee_id"]
        )
    ]
)
data class Follow(
    @Id
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    val id: UUID = UUID.randomUUID(),


    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val status: FollowStatus,

    @Column(nullable = false, updatable = false)
    val createdAt: Instant,


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "follower_id", nullable = false)
    val follower: User,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "followee_id", nullable = false)
    val followee: User
)