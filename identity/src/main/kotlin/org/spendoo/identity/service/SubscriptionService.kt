package org.spendoo.identity.service

import org.spendoo.identity.api.dto.request.SubscribePlanRequest
import org.spendoo.identity.api.dto.response.SubscriptionPlanResponse
import org.spendoo.identity.api.dto.response.toResponse
import org.spendoo.identity.entity.PlanCode
import org.spendoo.identity.entity.UserSubscription
import org.spendoo.identity.repository.SubscriptionPlanRepository
import org.spendoo.identity.repository.UserSubscriptionRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service

class SubscriptionService(
    private val subscriptionPlanRepository: SubscriptionPlanRepository,
    private val userSubscriptionRepository: UserSubscriptionRepository
) {

    @Transactional(readOnly = true)
    fun getAllPlans(userId: UUID, languageCode: String): List<SubscriptionPlanResponse> {

        val selectedPlanId = userSubscriptionRepository.findByUserId(userId)
            ?.subscriptionPlan
            ?.id

        return subscriptionPlanRepository.findAll()
            .map {
                it.toResponse(languageCode, isSelected = it.id == selectedPlanId)
            }
    }

    @Transactional
    fun assignPlan(userId: UUID, request: SubscribePlanRequest) {

        val targetPlan = subscriptionPlanRepository.findById(request.planId)
            .orElseThrow { IllegalArgumentException("Subscription plan not found") }

        val billingCycle =
            if (targetPlan.code == PlanCode.FREE) {
                null
            } else {
                request.billingCycle
                    ?: throw IllegalArgumentException("Billing cycle is required for paid plans")
            }

        val userSubscription = userSubscriptionRepository.findByUserId(userId)
            ?: UserSubscription(
                userId = userId,
                subscriptionPlan = targetPlan,
                billingCycle = billingCycle
            )

        val newUserSubscription = userSubscription.copy(
            subscriptionPlan = targetPlan,
            billingCycle  = billingCycle
        )

        userSubscriptionRepository.save(newUserSubscription )
    }

    @Transactional(readOnly = true)
    fun getCurrentPlan(userId: UUID): PlanCode {

        val activePlan = userSubscriptionRepository.findByUserId(userId)?.subscriptionPlan
            ?: subscriptionPlanRepository.findByCode(PlanCode.FREE)
            ?: throw IllegalStateException("FREE subscription plan missing from database")

        return activePlan.code
    }

}