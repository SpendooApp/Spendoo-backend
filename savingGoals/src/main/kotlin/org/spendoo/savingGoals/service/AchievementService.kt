package org.spendoo.savingGoals.service

import org.spendoo.savingGoals.api.dto.response.AchievementResponse
import org.spendoo.savingGoals.api.dto.response.toResponse
import org.spendoo.savingGoals.entity.Achievement
import org.spendoo.savingGoals.entity.AchievementType
import org.spendoo.savingGoals.entity.UserAchievement
import org.spendoo.savingGoals.repository.AchievementRepository
import org.spendoo.savingGoals.repository.SavingGoalRepository
import org.spendoo.savingGoals.repository.UserAchievementRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.util.*

@Service
class AchievementService(
    private val userAchievementRepository: UserAchievementRepository,
    private val achievementRepository: AchievementRepository,
    private val savingGoalRepository: SavingGoalRepository
) {

    @Transactional
    fun checkSpendooKingBadge(userId: UUID) {

        val targetAmountThreshold = BigDecimal("10000.00")
        val kingAchievement = achievementRepository.findByTargetValue(targetAmountThreshold) ?: return
        val totalSavings = savingGoalRepository.sumSavedAmountByUserId(userId) ?: BigDecimal.ZERO
        processAchievementProgress(userId, kingAchievement, totalSavings)
    }

    @Transactional
    fun checkCompletedGoalAchievements(userId: UUID) {

        val completedGoalsCount = BigDecimal(savingGoalRepository.countByUserIdAndIsCompletedTrue(userId))
        val goalThresholds = listOf(
            BigDecimal("5.00"),
            BigDecimal("10.00"),
            BigDecimal("15.00")
        )

        for (threshold in goalThresholds) {
            val achievement = achievementRepository.findByTargetValue(threshold) ?: continue
            processAchievementProgress(userId, achievement, completedGoalsCount)
        }
    }

    @Transactional
    fun checkPrioritySaverBadge(userId: UUID) {

        val priorityAchievement = achievementRepository.findByTitleEn("Priority Saver") ?: return
        val highPriorityCompletedCount = BigDecimal(savingGoalRepository.countCompletedHighPriorityGoals(userId))
        processAchievementProgress(userId, priorityAchievement, highPriorityCompletedCount)
    }

    @Transactional
    fun checkFirstStepBadge(userId: UUID) {

        val firstStepAchievement = achievementRepository.findByTitleEn("First Step") ?: return
        //this function is only called on the very first deposit, the progress is exactly 1
        val currentProgress = BigDecimal("1.00")
        processAchievementProgress(userId, firstStepAchievement, currentProgress)

    }

    @Transactional(readOnly = true)
    fun getAllAchievements(userId: UUID, pageable: Pageable, languageCode: String): Page<AchievementResponse> {
        val achievementPage = userAchievementRepository.findAllByUserId(userId, pageable)
        return achievementPage.map { it.toResponse(languageCode) }
    }

    @Transactional
    fun createDefaultAchievementsForUser(userId: UUID) {
        val defaultAchievements = listOf(
            Achievement(
                id = userId,
                targetValue = BigDecimal("10000.00"),
                level = 4,
                achievementType = AchievementType.SAVINGS,
                titleEn = "Spendoo King",
                titleAr = "ملك سبيندو",
                descriptionEn = "Reach a total savings of 10,000",
                descriptionAr = "وصول إجمالي مدخراتك إلى 10,000"
            ),
            Achievement(
                id = userId,
                targetValue = BigDecimal("1.00"),
                level = 1,
                achievementType = AchievementType.SAVINGS,
                titleEn = "First Step",
                titleAr = "الخطوة الاولى",
                descriptionEn = "add amount to saving balance for the first time",
                descriptionAr = "قم بإضافة الأموال إلى رصيد مدخراتك للمرة الأولى"
            ),
            Achievement(
                id = userId,
                targetValue = BigDecimal("5.00"),
                level = 2,
                achievementType = AchievementType.GOALS,
                titleEn = "High Five",
                titleAr = "الخماسية",
                descriptionEn = "complete 5 goals",
                descriptionAr = "أكمل 5 من أهداف الادخار الخاصة بك بنجاح"
            ),
            Achievement(
                id = userId,
                targetValue = BigDecimal("10.00"),
                level = 3,
                achievementType = AchievementType.GOALS,
                titleEn = "Double Five",
                titleAr = "عشرة على عشرة",
                descriptionEn = "complete 10 goals",
                descriptionAr = "أكمل 10 من أهداف الادخار الخاصة بك بنجاح"
            ),
            Achievement(
                id = userId,
                targetValue = BigDecimal("15.00"),
                level = 4,
                achievementType = AchievementType.GOALS,
                titleEn = "The Finisher",
                titleAr = "المنجز الاسطوري",
                descriptionEn = "complete 15 goals",
                descriptionAr = "أكمل 15 من أهداف الادخار الخاصة بك بنجاح"
            ),
            Achievement(
                id = userId,
                targetValue = BigDecimal("1.00"),
                level = 1,
                achievementType = AchievementType.GOALS,
                titleEn = "Priority Saver",
                titleAr = "مُدخر الاولويات",
                descriptionEn = "complete first goal that set with high priority",
                descriptionAr = "  أكمل هدف ذو أولوية عالية للمرة الأولى بنجاح"
            )
        )
        achievementRepository.saveAll(defaultAchievements)
    }

    private fun processAchievementProgress(userId: UUID, achievement: Achievement, currentProgress: BigDecimal) {
        val userAchievement = userAchievementRepository.findByUserIdAndAchievementId(userId, achievement.id)
            ?: UserAchievement(
                userId = userId,
                achievement = achievement,
                currentProgress = BigDecimal.ZERO,
                isUnlocked = false
            )

        // Only modify records if the user hasn't completed the badge already
        if (!userAchievement.isUnlocked) {
            userAchievement.currentProgress = currentProgress

            if (currentProgress >= achievement.targetValue) {
                userAchievement.isUnlocked = true
            }
            userAchievementRepository.save(userAchievement)
        }
    }
}