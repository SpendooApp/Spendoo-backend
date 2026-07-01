package org.spendoo.identity.seeds

import org.spendoo.identity.entity.PlanCode
import org.spendoo.identity.entity.SubscriptionPlan
import org.spendoo.identity.repository.SubscriptionPlanRepository
import org.springframework.boot.CommandLineRunner
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.util.UUID

@Component
class SubscriptionPlanSeeder(
    private val planRepository: SubscriptionPlanRepository
) : CommandLineRunner {
    override fun run(vararg args: String) {
        if (planRepository.count() == 0L) {
            val freePlan = SubscriptionPlan(
                id = UUID.fromString("11111111-1111-1111-1111-111111111111"),
                code = PlanCode.FREE,
                titleEn = "Free",
                titleAr = "مجاني",
                descriptionEn = "Standard expense tracking essentials",
                descriptionAr = "أساسيات تتبع المصروفات العادية",
                priceMonthly = BigDecimal.ZERO,
                priceYearly = BigDecimal.ZERO,
                isMostPopular = true,
                benefitsEn = listOf(
                    "Manual transactions: Unlimited",
                    "Receipt scanning (OCR): 5/month",
                    "Voice transactions: 10/month",
                    "History access: Daily / Weekly",
                    "Expense forecasting: Daily / Weekly",
                    "Categories: 50 max",
                    "AI Chatbot Limits: 15 messages / day",
                    "Savings suggestions"
                ),
                benefitsAr = listOf(
                    "المعاملات اليدوية: غير محدودة",
                    "مسح الفواتير : 5/شهريًا",
                    "التسجيل الصوتي : 10/ شهريًا",
                    "عرض السجل التاريخي: يومي / أسبوعي",
                    "توقع المصروفات: يومي / أسبوعي",
                    "الحد الأقصى للفئات: 50 فئة",
                    "الدردشة مع روبوت الذكي: 15 رسالة يوميًا",
                    "مقترحات وخطط التوفير المالي"
                )
            )

            val basicPlan = SubscriptionPlan(
                id = UUID.fromString("22222222-2222-2222-2222-222222222222"),
                code = PlanCode.BASIC,
                titleEn = "Basic",
                titleAr = "الأساسي",
                descriptionEn = "The essentials to get your finances on track",
                descriptionAr = "الأساسيات للحفاظ على استقرارك المالي",
                priceMonthly = BigDecimal(49.00),
                priceYearly = BigDecimal(469.00),
                isMostPopular = false,
                benefitsEn = listOf(
                    "Manual transactions: Unlimited",
                    "Receipt scanning (OCR): 30/month",
                    "Voice transactions: 50/month",
                    "Anomaly detection",
                    "History access: Monthly",
                    "Expense forecasting: Monthly",
                    "Categories: 150 max",
                    "AI Chatbot Limits: 50 messages / day",
                    "Savings suggestions"
                ),
                benefitsAr = listOf(
                    "المعاملات اليدوية: غير محدودة",
                    "مسح الفواتير : 30 شهريًا",
                    "التسجيل الصوتي : 50 شهريًا",
                    "مكتشف الانحرافات",
                    "عرض السجل التاريخي: شهري",
                    "توقع المصروفات: شهري",
                    "الحد الأقصى للفئات: 150 فئة",
                    "الدردشة مع روبوت الذكي: 50 رسالة يوميًا",
                    "مقترحات وخطط التوفير المالي"
                )
            )

            val proPlan = SubscriptionPlan(
                id = UUID.fromString("33333333-3333-3333-3333-333333333333"),
                code = PlanCode.PRO,
                titleEn = "Pro",
                titleAr = "برو المتميز",
                descriptionEn = "Let Spendoo's AI manage your money for you",
                descriptionAr = "دع روبوت سبيندو يدير أموالك بالكامل",
                priceMonthly = BigDecimal(99.00),
                priceYearly = BigDecimal(949.00),
                isMostPopular = false,
                benefitsEn = listOf(
                    "Manual transactions: Unlimited",
                    "Receipt scanning (OCR): Unlimited",
                    "Voice transactions: Unlimited",
                    "Anomaly detection",
                    "History access: Yearly (All time)",
                    "Expense forecasting: Yearly (All granularities)",
                    "Categories: Unlimited",
                    "AI Chatbot Limits: 150 messages / day",
                    "Savings suggestions"
                ),
                benefitsAr = listOf(
                    "المعاملات اليدوية: غير محدودة",
                    "مسح الفواتير : غير محدود",
                    "التسجيل الصوتي : غير محدود",
                    "مكتشف الانحرافات ",
                    "عرض السجل التاريخي: سنوي ",
                    "توقع المصروفات: سنوي (بجميع مستويات التفصيل)",
                    "الحد الأقصى للفئات: غير محدود",
                    "الدردشة مع روبوت الذكي : 150 رسالة يوميًا",
                    "مقترحات وخطط التوفير المالي"
                )
            )

            planRepository.saveAll(listOf(freePlan, basicPlan, proPlan))
        }
    }

}