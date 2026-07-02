package org.spendoo.identity.entity

import jakarta.persistence.*
import java.util.UUID

@Entity
@Table(name = "user_subscriptions", schema = "identity" )
data class UserSubscription(
    @Id
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    val id: UUID = UUID.randomUUID(),

    @Column(columnDefinition = "uuid", nullable = false, unique = true)
    val userId: UUID = UUID.randomUUID(),

    @Enumerated(EnumType.STRING)
    @Column
    val billingCycle: BillingCycle?,

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(nullable = false)
    val subscriptionPlan: SubscriptionPlan

)
