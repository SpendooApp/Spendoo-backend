package org.spendoo.identity.integration

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.spendoo.identity.IdentityTestApplication
import org.spendoo.identity.api.dto.request.SubscribePlanRequest
import org.spendoo.identity.entity.BillingCycle
import org.spendoo.identity.entity.PlanCode
import org.spendoo.identity.entity.SubscriptionPlan
import org.spendoo.identity.entity.UserSubscription
import org.spendoo.identity.repository.SubscriptionPlanRepository
import org.spendoo.identity.repository.UserSubscriptionRepository
import org.spendoo.identity.service.SubscriptionService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import java.math.BigDecimal
import java.util.*

@SpringBootTest(classes = [IdentityTestApplication::class])
@ActiveProfiles("test")
class SubscriptionServiceIntegrationTest {

    @Autowired
    private lateinit var subscriptionService: SubscriptionService

    @Autowired
    private lateinit var subscriptionPlanRepository: SubscriptionPlanRepository

    @Autowired
    private lateinit var userSubscriptionRepository: UserSubscriptionRepository

    private lateinit var freePlan: SubscriptionPlan

    private lateinit var proPlan: SubscriptionPlan

    private val testUserId: UUID = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        userSubscriptionRepository.deleteAll()
        subscriptionPlanRepository.deleteAll()

        freePlan = subscriptionPlanRepository.save(
            SubscriptionPlan(
                id = UUID.fromString("11111111-1111-1111-1111-111111111111"),
                code = PlanCode.FREE,
                titleEn = "Free",
                titleAr = "مجاني",
                descriptionEn = "Standard expense tracking essentials",
                descriptionAr = "أساسيات تتبع المصروفات العادية",
                priceMonthly = BigDecimal.ZERO,
                priceYearly = BigDecimal.ZERO,
                isMostPopular = false
            )
        )

        proPlan = subscriptionPlanRepository.save(
            SubscriptionPlan(
                id = UUID.fromString("33333333-3333-3333-3333-333333333333"),
                code = PlanCode.PRO,
                titleEn = "Pro",
                titleAr = "برو المتميز",
                descriptionEn = "Let Spendoo's AI manage your money for you",
                descriptionAr = "دع ذكاء سبيندو يدير أموالك بالكامل",
                priceMonthly = BigDecimal(99.00),
                priceYearly = BigDecimal(949.00),
                isMostPopular = false
            )
        )
    }

    @Test
    fun `getAllPlans returns all plans and flags selected plan correctly`() {

        userSubscriptionRepository.save(
            UserSubscription(
                userId = testUserId,
                subscriptionPlan = freePlan,
                billingCycle = null
            )
        )

        val plans = subscriptionService.getAllPlans(testUserId, "en")

        // Assert
        assertThat(plans).hasSize(2)
        val freePlanResponse = plans.find { it.id == freePlan.id }
        val proPlanResponse = plans.find { it.id == proPlan.id }

        assertThat(freePlanResponse?.isSelected).isTrue()
        assertThat(proPlanResponse?.isSelected).isFalse()
    }

    @Test
    fun `assignPlan creates new subscription when none exists`() {

        val request = SubscribePlanRequest(planId = freePlan.id, billingCycle = null)

        subscriptionService.assignPlan(testUserId, request)

        val savedSubscription = userSubscriptionRepository.findByUserId(testUserId)
        assertThat(savedSubscription).isNotNull()
        assertThat(savedSubscription?.subscriptionPlan?.id).isEqualTo(freePlan.id)
        assertThat(savedSubscription?.billingCycle).isNull()
    }

    @Test
    fun `assignPlan updates existing plan to a new selection successfully`() {

        userSubscriptionRepository.save(
            UserSubscription(
                userId = testUserId,
                subscriptionPlan = freePlan,
                billingCycle = null
            )
        )
        val upgradeRequest = SubscribePlanRequest(planId = proPlan.id, billingCycle = BillingCycle.MONTHLY)

        subscriptionService.assignPlan(testUserId, upgradeRequest)

        val updatedSubscription = userSubscriptionRepository.findByUserId(testUserId)
        assertThat(updatedSubscription).isNotNull()
        assertThat(updatedSubscription?.subscriptionPlan?.id).isEqualTo(proPlan.id)
        assertThat(updatedSubscription?.billingCycle).isEqualTo(BillingCycle.MONTHLY)
    }

    @Test
    fun `assignPlan throws IllegalArgumentException if target plan id does not exist`() {

        val invalidRequest = SubscribePlanRequest(planId = UUID.randomUUID(), billingCycle = null)

        val exception = assertThrows<IllegalArgumentException> {
            subscriptionService.assignPlan(testUserId, invalidRequest)
        }
        assertThat(exception).hasMessageThat().contains("Subscription plan not found")
    }

    @Test
    fun `assignPlan throws IllegalArgumentException if paid plan request lacks billing cycle`() {

        val badRequest = SubscribePlanRequest(planId = proPlan.id, billingCycle = null)

        val exception = assertThrows<IllegalArgumentException> {
            subscriptionService.assignPlan(testUserId, badRequest)
        }
        assertThat(exception).hasMessageThat().contains("Billing cycle is required for paid plans")
    }
}