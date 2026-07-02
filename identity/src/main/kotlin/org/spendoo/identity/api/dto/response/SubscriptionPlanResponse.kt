package org.spendoo.identity.api.dto.response

import org.spendoo.identity.entity.PlanCode
import org.spendoo.identity.entity.SubscriptionPlan
import java.math.BigDecimal
import java.util.UUID

data class SubscriptionPlanResponse(
    val id : UUID,
    val title : String,
    val description : String,
    val code: PlanCode,
    val priceMonthly: BigDecimal,
    val priceYearly: BigDecimal,
    val isMostPopular: Boolean,
    val benefits: List<String>,
    val isSelected: Boolean
)

fun SubscriptionPlan.toResponse(languageCode: String,isSelected: Boolean): SubscriptionPlanResponse {
    val useArabic = languageCode.lowercase().startsWith("ar")
    return SubscriptionPlanResponse(
        id = id,
        title = if (useArabic) titleAr else titleEn,
        description = if (useArabic) descriptionAr else descriptionEn,
        code = code,
        priceMonthly = priceMonthly,
        priceYearly = priceYearly,
        isMostPopular = isMostPopular,
        benefits = if (useArabic) benefitsAr else benefitsEn,
        isSelected = isSelected
    )
}
