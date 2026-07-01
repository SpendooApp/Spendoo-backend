package org.spendoo.identity.repository

import org.spendoo.identity.entity.PlanCode
import org.spendoo.identity.entity.SubscriptionPlan
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface SubscriptionPlanRepository: JpaRepository<SubscriptionPlan, UUID> {
    fun findByCode(code: PlanCode): SubscriptionPlan?
}