package org.spendoo.savingGoals.service

import org.spendoo.events.achievements.AchievementEarnedEvent
import org.spendoo.events.publisher.SpendooEventPublisher
import org.spendoo.savingGoals.api.dto.response.AchievementResponse
import org.spendoo.savingGoals.api.dto.response.toResponse
import org.spendoo.savingGoals.entity.*
import org.spendoo.savingGoals.repository.*
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDate
import java.util.*
import org.spendoo.events.achievements.utils.AchievementType as EventAchievementType

@Service
class AchievementService(
    private val userAchievementRepository: UserAchievementRepository,
    private val achievementRepository: AchievementRepository,
    private val savingGoalRepository: SavingGoalRepository,
    private val savingBalanceRepository: SavingBalanceRepository,
    private val loginStreakRepository: LoginStreakRepository,
    private val transactionStreakRepository: TransactionStreakRepository,
    private val eventPublisher: SpendooEventPublisher
) {

    @Transactional
    fun seedDefaultAchievementsIfEmpty() {
        val existingCodes = achievementRepository.findAllCodes().toSet()

        val defaultAchievements = getAllAchievements()

        val toSave = defaultAchievements.filter { it.code !in existingCodes }
        if (toSave.isNotEmpty()) {
            achievementRepository.saveAll(toSave)
        }
    }

    @Transactional
    fun ensureUserAchievementsCreated(userId: UUID) {
        seedDefaultAchievementsIfEmpty()

        var page = 0
        val pageSize = 100
        do {
            val missingPage =
                achievementRepository.findMissingAchievementsForUser(userId, PageRequest.of(page, pageSize))
            val newLinks = missingPage.content.map { achievement ->
                UserAchievement(
                    userId = userId,
                    achievement = achievement,
                    currentProgress = BigDecimal.ZERO,
                    isUnlocked = false
                )
            }
            userAchievementRepository.saveAll(newLinks)
            page++
        } while (missingPage.hasNext() && missingPage.content.isNotEmpty())
    }

    @Transactional
    fun syncMissingAchievementsForAllUsers() {
        seedDefaultAchievementsIfEmpty()

        var page = 0
        val pageSize = 100
        do {
            val userIdsPage = userAchievementRepository.findDistinctUserIds(PageRequest.of(page, pageSize))
            for (userId in userIdsPage.content) {
                ensureUserAchievementsCreated(userId)
            }
            page++
        } while (userIdsPage.hasNext() && userIdsPage.content.isNotEmpty())
    }

    @Transactional
    fun handleUserLoggedIn(userId: UUID) {
        val today = LocalDate.now()
        if (loginStreakRepository.existsByUserIdAndStreakDate(userId, today)) return

        val yesterday = today.minusDays(1)
        val hasStreakYesterday = loginStreakRepository.existsByUserIdAndStreakDate(userId, yesterday)

        if (!hasStreakYesterday) {
            loginStreakRepository.deleteAllByUserId(userId)
        }

        loginStreakRepository.save(LoginStreak(userId = userId, streakDate = today))

        val streakCount = loginStreakRepository.countByUserId(userId)
        checkLoginStreakAchievements(userId, streakCount)
    }

    @Transactional
    fun handleTransactionCreated(userId: UUID, distinctCategoryCount: Int) {
        val today = LocalDate.now()
        if (transactionStreakRepository.existsByUserIdAndStreakDate(userId, today)) {
            checkCategoryAchievements(userId, distinctCategoryCount)
            return
        }

        val yesterday = today.minusDays(1)
        val hasStreakYesterday = transactionStreakRepository.existsByUserIdAndStreakDate(userId, yesterday)

        if (!hasStreakYesterday) {
            transactionStreakRepository.deleteAllByUserId(userId)
        }

        transactionStreakRepository.save(TransactionStreak(userId = userId, streakDate = today))

        val streakCount = transactionStreakRepository.countByUserId(userId)
        checkTransactionStreakAchievements(userId, streakCount)
        checkCategoryAchievements(userId, distinctCategoryCount)
    }

    @Transactional
    fun checkSavingsAchievements(userId: UUID, isFirstDeposit: Boolean) {
        val totalSavings = savingGoalRepository.sumSavedAmountByUserId(userId) ?: BigDecimal.ZERO
        val hasSaved = savingBalanceRepository.existsByUserId(userId)

        val userAchievements =
            userAchievementRepository.findAllByUserIdAndAchievementAchievementTypeAndIsUnlockedIsFalse(
                userId,
                AchievementType.SAVINGS
            )
        for (userAchievement in userAchievements) {
            val progress = when (userAchievement.achievement.code) {
                AchievementCode.SAVINGS_L1 -> if (isFirstDeposit || hasSaved) BigDecimal(1.0) else BigDecimal.ZERO
                AchievementCode.SAVINGS_L2,
                AchievementCode.SAVINGS_L3,
                AchievementCode.SAVINGS_L4 -> totalSavings

                else -> userAchievement.currentProgress
            }
            updateProgressAndUnlockIfNeeded(userAchievement, progress)
        }
    }

    @Transactional
    fun checkGoalAchievements(userId: UUID) {
        val completedCount = BigDecimal(savingGoalRepository.countByUserIdAndIsCompletedTrue(userId))
        val highPriorityCount =
            BigDecimal(savingGoalRepository.countByUserIdAndIsCompletedTrueAndPriorityGreaterThanEqual(userId, 3))

        val userAchievements =
            userAchievementRepository.findAllByUserIdAndAchievementAchievementTypeAndIsUnlockedIsFalse(
                userId,
                AchievementType.GOALS
            )
        for (userAchievement in userAchievements) {
            val progress = when (userAchievement.achievement.code) {
                AchievementCode.PRIORITY_SAVER -> highPriorityCount
                AchievementCode.HIGH_FIVE,
                AchievementCode.DOUBLE_FIVE,
                AchievementCode.THE_FINISHER -> completedCount

                else -> userAchievement.currentProgress
            }
            updateProgressAndUnlockIfNeeded(userAchievement, progress)
        }
    }

    @Transactional
    fun checkLoginStreakAchievements(userId: UUID, count: Long) {
        val progress = BigDecimal(count)
        val userAchievements =
            userAchievementRepository.findAllByUserIdAndAchievementAchievementTypeAndIsUnlockedIsFalse(
                userId,
                AchievementType.LOGIN_STREAK
            )
        for (userAchievement in userAchievements) {
            updateProgressAndUnlockIfNeeded(userAchievement, progress)
        }
    }

    @Transactional
    fun checkTransactionStreakAchievements(userId: UUID, count: Long) {
        val progress = BigDecimal(count)
        val userAchievements =
            userAchievementRepository.findAllByUserIdAndAchievementAchievementTypeAndIsUnlockedIsFalse(
                userId,
                AchievementType.TRANSACTION_STREAK
            )
        for (userAchievement in userAchievements) {
            updateProgressAndUnlockIfNeeded(userAchievement, progress)
        }
    }

    @Transactional
    fun checkCategoryAchievements(userId: UUID, distinctCategoryCount: Int) {
        val progress = BigDecimal(distinctCategoryCount)
        val userAchievements =
            userAchievementRepository.findAllByUserIdAndAchievementAchievementTypeAndIsUnlockedIsFalse(
                userId,
                AchievementType.CATEGORIES
            )
        for (userAchievement in userAchievements) {
            updateProgressAndUnlockIfNeeded(userAchievement, progress)
        }
    }

    @Transactional
    fun getAllAchievements(userId: UUID, pageable: Pageable, languageCode: String): Page<AchievementResponse> {
        ensureUserAchievementsCreated(userId)
        val achievementPage = userAchievementRepository.findAllByUserId(userId, pageable)
        return achievementPage.map { it.toResponse(languageCode) }
    }

    private fun updateProgressAndUnlockIfNeeded(userAchievement: UserAchievement, currentProgress: BigDecimal) {
        userAchievement.currentProgress = currentProgress

        if (currentProgress >= userAchievement.achievement.targetValue) {
            userAchievement.isUnlocked = true
            userAchievementRepository.save(userAchievement)

            eventPublisher.publish(
                AchievementEarnedEvent(
                    achievementId = userAchievement.achievement.id,
                    userId = userAchievement.userId,
                    titleEn = userAchievement.achievement.titleEn,
                    titleAr = userAchievement.achievement.titleAr,
                    descriptionEn = userAchievement.achievement.descriptionEn,
                    descriptionAr = userAchievement.achievement.descriptionAr,
                    level = userAchievement.achievement.level,
                    targetValue = userAchievement.achievement.targetValue,
                    achievementType = EventAchievementType.fromStringOrDefault(userAchievement.achievement.achievementType.name)
                )
            )
        } else {
            userAchievement.saveProgress(currentProgress)
        }
    }

    private fun UserAchievement.saveProgress(progress: BigDecimal) {
        this.currentProgress = progress
        userAchievementRepository.save(this)
    }

    private companion object {
        fun getGoalAchievements() = listOf(
            Achievement(
                id = UUID.randomUUID(),
                code = AchievementCode.PRIORITY_SAVER,
                targetValue = BigDecimal(1.0),
                level = 1,
                achievementType = AchievementType.GOALS,
                titleEn = "Priority Saver",
                titleAr = "مُدخر الاولويات",
                descriptionEn = "complete first goal that set with high priority",
                descriptionAr = "  أكمل هدف ذو أولوية عالية للمرة الأولى بنجاح"
            ),
            Achievement(
                id = UUID.randomUUID(),
                code = AchievementCode.HIGH_FIVE,
                targetValue = BigDecimal(5.0),
                level = 2,
                achievementType = AchievementType.GOALS,
                titleEn = "High Five",
                titleAr = "الخماسية",
                descriptionEn = "complete 5 goals",
                descriptionAr = "أكمل 5 من أهداف الادخار الخاصة بك بنجاح"
            ),
            Achievement(
                id = UUID.randomUUID(),
                code = AchievementCode.DOUBLE_FIVE,
                targetValue = BigDecimal(10.0),
                level = 3,
                achievementType = AchievementType.GOALS,
                titleEn = "Double Five",
                titleAr = "عشرة على عشرة",
                descriptionEn = "complete 10 goals",
                descriptionAr = "أكمل 10 من أهداف الادخار الخاصة بك بنجاح"
            ),
            Achievement(
                id = UUID.randomUUID(),
                code = AchievementCode.THE_FINISHER,
                targetValue = BigDecimal(15.0),
                level = 4,
                achievementType = AchievementType.GOALS,
                titleEn = "The Finisher",
                titleAr = "المنجز الاسطوري",
                descriptionEn = "complete 15 goals",
                descriptionAr = "أكمل 15 من أهداف الادخار الخاصة بك بنجاح"
            )
        )

        fun getSavingAchievements() = listOf(
            Achievement(
                id = UUID.randomUUID(),
                code = AchievementCode.SAVINGS_L1,
                targetValue = BigDecimal(1.0),
                level = 1,
                achievementType = AchievementType.SAVINGS,
                titleEn = "First Step",
                titleAr = "الخطوة الاولى",
                descriptionEn = "add amount to saving balance for the first time",
                descriptionAr = "قم بإضافة الأموال إلى رصيد مدخراتك للمرة الأولى"
            ),
            Achievement(
                id = UUID.randomUUID(),
                code = AchievementCode.SAVINGS_L2,
                targetValue = BigDecimal(500.0),
                level = 2,
                achievementType = AchievementType.SAVINGS,
                titleEn = "Pocket Saver",
                titleAr = "مدخر الجيب",
                descriptionEn = "Reach a total savings of 500",
                descriptionAr = "وصول إجمالي مدخراتك إلى 500"
            ),
            Achievement(
                id = UUID.randomUUID(),
                code = AchievementCode.SAVINGS_L3,
                targetValue = BigDecimal(2500.0),
                level = 3,
                achievementType = AchievementType.SAVINGS,
                titleEn = "Smart Saver",
                titleAr = "المدخر الذكي",
                descriptionEn = "Reach a total savings of 2,500",
                descriptionAr = "وصول إجمالي مدخراتك إلى 2,500"
            ),
            Achievement(
                id = UUID.randomUUID(),
                code = AchievementCode.SAVINGS_L4,
                targetValue = BigDecimal(10000.0),
                level = 4,
                achievementType = AchievementType.SAVINGS,
                titleEn = "Spendoo King",
                titleAr = "ملك سبيندو",
                descriptionEn = "Reach a total savings of 10,000",
                descriptionAr = "وصول إجمالي مدخراتك إلى 10,000"
            )
        )

        fun getLoginStreakAchievements() = listOf(
            Achievement(
                id = UUID.randomUUID(),
                code = AchievementCode.LOGIN_STREAK_L1,
                targetValue = BigDecimal(3.0),
                level = 1,
                achievementType = AchievementType.LOGIN_STREAK,
                titleEn = "Streak Starter",
                titleAr = "بادئ السلسلة",
                descriptionEn = "Log in for 3 consecutive days",
                descriptionAr = "سلسلة تسجيل الدخول لثلاثة أيام متتالية"
            ),
            Achievement(
                id = UUID.randomUUID(),
                code = AchievementCode.LOGIN_STREAK_L2,
                targetValue = BigDecimal(7.0),
                level = 2,
                achievementType = AchievementType.LOGIN_STREAK,
                titleEn = "Habit Builder",
                titleAr = "باني العادات",
                descriptionEn = "Log in for 7 consecutive days",
                descriptionAr = "سلسلة تسجيل الدخول لسبعة أيام متتالية"
            ),
            Achievement(
                id = UUID.randomUUID(),
                code = AchievementCode.LOGIN_STREAK_L3,
                targetValue = BigDecimal(15.0),
                level = 3,
                achievementType = AchievementType.LOGIN_STREAK,
                titleEn = "Dedication Master",
                titleAr = "خبير الالتزام",
                descriptionEn = "Log in for 15 consecutive days",
                descriptionAr = "سلسلة تسجيل الدخول لخمسة عشر يوماً متتالياً"
            ),
            Achievement(
                id = UUID.randomUUID(),
                code = AchievementCode.LOGIN_STREAK_L4,
                targetValue = BigDecimal(30.0),
                level = 4,
                achievementType = AchievementType.LOGIN_STREAK,
                titleEn = "Spendoo Devotee",
                titleAr = "المخلص لسبيندو",
                descriptionEn = "Log in for 30 consecutive days",
                descriptionAr = "سلسلة تسجيل الدخول لثلاثين يوماً متتالياً"
            )
        )

        fun getTransactionStreakAchievements() = listOf(
            Achievement(
                id = UUID.randomUUID(),
                code = AchievementCode.TRANSACTION_STREAK_L1,
                targetValue = BigDecimal(3.0),
                level = 1,
                achievementType = AchievementType.TRANSACTION_STREAK,
                titleEn = "Active Tracker",
                titleAr = "المتابع النشط",
                descriptionEn = "Record a transaction for 3 consecutive days",
                descriptionAr = "تسجيل الحركات لثلاثة أيام متتالية"
            ),
            Achievement(
                id = UUID.randomUUID(),
                code = AchievementCode.TRANSACTION_STREAK_L2,
                targetValue = BigDecimal(7.0),
                level = 2,
                achievementType = AchievementType.TRANSACTION_STREAK,
                titleEn = "Expense Tracker",
                titleAr = "متعقب المصاريف",
                descriptionEn = "Record a transaction for 7 consecutive days",
                descriptionAr = "تسجيل الحركات لسبعة أيام متتالية"
            ),
            Achievement(
                id = UUID.randomUUID(),
                code = AchievementCode.TRANSACTION_STREAK_L3,
                targetValue = BigDecimal(15.0),
                level = 3,
                achievementType = AchievementType.TRANSACTION_STREAK,
                titleEn = "Streak Specialist",
                titleAr = "أخصائي السلسلة",
                descriptionEn = "Record a transaction for 15 consecutive days",
                descriptionAr = "تسجيل الحركات لخمسة عشر يوماً متتالياً"
            ),
            Achievement(
                id = UUID.randomUUID(),
                code = AchievementCode.TRANSACTION_STREAK_L4,
                targetValue = BigDecimal(30.0),
                level = 4,
                achievementType = AchievementType.TRANSACTION_STREAK,
                titleEn = "Financially Aware",
                titleAr = "الواعي مالياً",
                descriptionEn = "Record a transaction for 30 consecutive days",
                descriptionAr = "تسجيل الحركات لثلاثين يوماً متتالياً"
            )
        )

        fun getCategoryAchievements() = listOf(
            Achievement(
                id = UUID.randomUUID(),
                code = AchievementCode.CATEGORY_L1,
                targetValue = BigDecimal(3.0),
                level = 1,
                achievementType = AchievementType.CATEGORIES,
                titleEn = "Category Rookie",
                titleAr = "مبتدئ الفئات",
                descriptionEn = "Log transactions in 3 different categories",
                descriptionAr = "تسجيل الحركات في 3 فئات مختلفة"
            ),
            Achievement(
                id = UUID.randomUUID(),
                code = AchievementCode.CATEGORY_L2,
                targetValue = BigDecimal(5.0),
                level = 2,
                achievementType = AchievementType.CATEGORIES,
                titleEn = "Budget Explorer",
                titleAr = "مستكشف الميزانية",
                descriptionEn = "Log transactions in 5 different categories",
                descriptionAr = "تسجيل الحركات في 5 فئات مختلفة"
            ),
            Achievement(
                id = UUID.randomUUID(),
                code = AchievementCode.CATEGORY_L3,
                targetValue = BigDecimal(8.0),
                level = 3,
                achievementType = AchievementType.CATEGORIES,
                titleEn = "Finance Organizer",
                titleAr = "منظم المالية",
                descriptionEn = "Log transactions in 8 different categories",
                descriptionAr = "تسجيل الحركات في 8 فئات مختلفة"
            ),
            Achievement(
                id = UUID.randomUUID(),
                code = AchievementCode.CATEGORY_L4,
                targetValue = BigDecimal(12.0),
                level = 4,
                achievementType = AchievementType.CATEGORIES,
                titleEn = "Portfolio Master",
                titleAr = "سيد المحفظة",
                descriptionEn = "Log transactions in 12 different categories",
                descriptionAr = "تسجيل الحركات في 12 فئة مختلفة"
            )
        )

        fun getAllAchievements() =
            getGoalAchievements() + getSavingAchievements() + getLoginStreakAchievements() + getTransactionStreakAchievements() + getCategoryAchievements()
    }
}