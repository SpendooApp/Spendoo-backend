package org.spendoo.identity.api.controller

import jakarta.validation.Valid
import org.spendoo.identity.api.dto.request.SubscribePlanRequest
import org.spendoo.identity.api.dto.response.SubscriptionPlanResponse
import org.spendoo.identity.service.SubscriptionService
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/api/v1/subscriptions")
class SubscriptionController(
    private val subscriptionService: SubscriptionService
) {
    @GetMapping("/plans")
    fun getSubscriptionPlans(
        @AuthenticationPrincipal userId: UUID,
        @RequestHeader(name = "Accept-Language", defaultValue = "en") languageCode: String
    ): ResponseEntity<List<SubscriptionPlanResponse>> {
        val plans = subscriptionService.getAllPlans(userId, languageCode)
        return ResponseEntity.ok(plans)
    }

    @PostMapping("/subscribe")
    fun subscribePlan(
        @AuthenticationPrincipal userId: UUID,
        @Valid @RequestBody request: SubscribePlanRequest
    ): ResponseEntity<Unit> {
        subscriptionService.assignPlan(userId, request)
        return ResponseEntity.ok().build()
    }
}