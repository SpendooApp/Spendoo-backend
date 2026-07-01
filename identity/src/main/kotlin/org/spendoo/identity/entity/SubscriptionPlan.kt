package org.spendoo.identity.entity

import jakarta.persistence.*
import java.math.BigDecimal
import java.util.UUID

@Entity
@Table(name = "subscription_plans", schema = "identity" )
data class SubscriptionPlan(
    @Id
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    val id: UUID = UUID.randomUUID(),

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val code: PlanCode,

    @Column(nullable = false)
    val titleEn: String,

    @Column(nullable = false)
    val titleAr: String,

    @Column(nullable = false)
    val descriptionEn: String,

    @Column(nullable = false)
    val descriptionAr: String,

    @Column(nullable = false)
    val priceMonthly: BigDecimal,

    @Column(nullable = false)
    val priceYearly: BigDecimal,

    @Column(nullable = false)
    val isMostPopular: Boolean = false,

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
        name = "subscription_plan_benefits_en",
        joinColumns = [JoinColumn(name = "id")],
        schema = "identity"
    )
    @Column(nullable = false)
    val benefitsEn: List<String> = emptyList(),

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
        name = "subscription_plan_benefits_ar",
        joinColumns = [JoinColumn(name = "id")],
        schema = "identity"
    )
    @Column(nullable = false)
    val benefitsAr: List<String> = emptyList()
)
