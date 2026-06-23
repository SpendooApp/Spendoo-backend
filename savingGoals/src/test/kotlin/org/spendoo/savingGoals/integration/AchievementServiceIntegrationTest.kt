package org.spendoo.savingGoals.integration

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.spendoo.savingGoals.SavingGoalsTestApplication
import org.spendoo.savingGoals.api.dto.request.AssignAmountRequest
import org.spendoo.savingGoals.entity.*
import org.spendoo.savingGoals.repository.*
import org.spendoo.savingGoals.service.AchievementService
import org.spendoo.savingGoals.service.SavingGoalService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.domain.PageRequest
import org.springframework.test.context.ActiveProfiles
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.*


@SpringBootTest(classes = [SavingGoalsTestApplication::class])
@ActiveProfiles("test")
class AchievementServiceIntegrationTest {

    @Autowired
    private lateinit var achievementService: AchievementService

    @Autowired
    private lateinit var savingGoalService: SavingGoalService

    @Autowired
    private lateinit var achievementRepository: AchievementRepository

    @Autowired
    private lateinit var userAchievementRepository: UserAchievementRepository

    @Autowired
    private lateinit var savingGoalRepository: SavingGoalRepository

    @Autowired
    private lateinit var savingBalanceRepository: SavingBalanceRepository

    @Autowired
    private lateinit var savingGoalHistoryRepository: SavingGoalHistoryRepository

    private val userId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        savingGoalHistoryRepository.deleteAll()
        userAchievementRepository.deleteAll()
        savingGoalRepository.deleteAll()
        savingBalanceRepository.deleteAll()
        achievementRepository.deleteAll()

       seedDefaultAchievements()
    }

    private fun seedDefaultAchievements() {
        val defaultAchievements = listOf(
            Achievement(
                id = UUID.randomUUID(),
                targetValue = BigDecimal("10000.00"),
                level = 4,
                achievementType = AchievementType.SAVINGS,
                titleEn = "Spendoo King",
                titleAr = "ملك سبيندو",
                descriptionEn = "Reach a total savings of 10,000",
                descriptionAr = "وصول إجمالي مدخراتك إلى 10,000"
            ),
            Achievement(
                id = UUID.randomUUID(),
                targetValue = BigDecimal("1.00"),
                level = 1,
                achievementType = AchievementType.SAVINGS,
                titleEn = "First Step",
                titleAr = "الخطوة الاولى",
                descriptionEn = "add amount to saving balance for the first time",
                descriptionAr = "قم بإضافة الأموال إلى رصيد مدخراتك للمرة الأولى"
            ),
            Achievement(
                id = UUID.randomUUID(),
                targetValue = BigDecimal("5.00"),
                level = 2,
                achievementType = AchievementType.GOALS,
                titleEn = "High Five",
                titleAr = "الخماسية",
                descriptionEn = "complete 5 goals",
                descriptionAr = "أكمل 5 من أهداف الادخار الخاصة بك بنجاح"
            ),
            Achievement(
                id = UUID.randomUUID(),
                targetValue = BigDecimal("10.00"),
                level = 3,
                achievementType = AchievementType.GOALS,
                titleEn = "Double Five",
                titleAr = "عشرة على عشرة",
                descriptionEn = "complete 10 goals",
                descriptionAr = "أكمل 10 من أهداف الادخار الخاصة بك بنجاح"
            ),
            Achievement(
                id = UUID.randomUUID(),
                targetValue = BigDecimal("15.00"),
                level = 4,
                achievementType = AchievementType.GOALS,
                titleEn = "The Finisher",
                titleAr = "المنجز الاسطوري",
                descriptionEn = "complete 15 goals",
                descriptionAr = "أكمل 15 من أهداف الادخار الخاصة بك بنجاح"
            ),
            Achievement(
                id = UUID.randomUUID(),
                targetValue = BigDecimal("1.00"),
                level = 1,
                achievementType = AchievementType.GOALS,
                titleEn = "Priority Saver",
                titleAr = "مُدخر الاولويات",
                descriptionEn = "complete first goal that set with high priority",
                descriptionAr = "أكمل هدف ذو أولوية عالية للمرة الأولى بنجاح"
            )
        )
        achievementRepository.saveAll(defaultAchievements)
    }

    @Test
    fun `addToSavings awards First Step badge on first deposit`() {
        val depositAmount = BigDecimal("150.00")

        savingGoalService.addToSavings(userId, depositAmount)

        val userAchievements = userAchievementRepository.findAllByUserId(userId, PageRequest.of(0, 10)).content
        val firstStepBadge = userAchievements.find { it.achievement.titleEn == "First Step" }

        assertThat(firstStepBadge).isNotNull()
        assertThat(firstStepBadge!!.isUnlocked).isTrue()
        assertThat(firstStepBadge.currentProgress.compareTo(BigDecimal("1.00"))).isEqualTo(0)
    }

    @Test
    fun `addToSavings does not unlock duplicate First Step badges on subsequent deposits`() {
        // Deposit twice
        savingGoalService.addToSavings(userId, BigDecimal("100.00"))
        savingGoalService.addToSavings(userId, BigDecimal("200.00"))

        // Ensure exactly one achievement entry exists
        val userAchievements = userAchievementRepository.findAllByUserId(userId, PageRequest.of(0, 10)).content
        val firstStepMatches = userAchievements.filter { it.achievement.titleEn == "First Step" }

        assertThat(firstStepMatches).hasSize(1)
        assertThat(firstStepMatches.first().isUnlocked).isTrue()
    }

    @Test
    fun `assignAmountToGoal awards Spendoo King badge when cumulative savings history reaches 10000`() {

        savingBalanceRepository.save(SavingBalance(userId = userId, unassignedAmount = BigDecimal("12000.00")))
        val goal = createAndSaveGoal(targetAmount = BigDecimal("10000.00"), priority = 2)


        savingGoalService.assignAmountToGoal(goal.id, userId, AssignAmountRequest(amount = BigDecimal("10000.00")))

        val userAchievements = userAchievementRepository.findAllByUserId(userId, PageRequest.of(0, 10)).content
        val kingBadge = userAchievements.find { it.achievement.titleEn == "Spendoo King" }

        assertThat(kingBadge).isNotNull()
        assertThat(kingBadge!!.isUnlocked).isTrue()
        assertThat(kingBadge.currentProgress.compareTo(BigDecimal("10000.00"))).isEqualTo(0)
    }

    @Test
    fun `assignAmountToGoal awards High Five badge when completing 5 goals`() {

        savingBalanceRepository.save(SavingBalance(userId = userId, unassignedAmount = BigDecimal("5000.00")))

        // Complete 4 goals
        for (i in 1..4) {
            val goal = createAndSaveGoal(targetAmount = BigDecimal("100.00"), priority = 2)
            savingGoalService.assignAmountToGoal(goal.id, userId, AssignAmountRequest(amount = BigDecimal("100.00")))
        }

        var userAchievements = userAchievementRepository.findAllByUserId(userId, PageRequest.of(0, 10)).content
        var highFiveBadge = userAchievements.find { it.achievement.titleEn == "High Five" }
        assertThat(highFiveBadge?.isUnlocked ?: false).isFalse()

        // Complete the 5th goal
        val finalGoal = createAndSaveGoal(targetAmount = BigDecimal("100.00"), priority = 2)
        savingGoalService.assignAmountToGoal(finalGoal.id, userId, AssignAmountRequest(amount = BigDecimal("100.00")))

        //  High Five badge is unlocked dynamically
        userAchievements = userAchievementRepository.findAllByUserId(userId, PageRequest.of(0, 10)).content
        highFiveBadge = userAchievements.find { it.achievement.titleEn == "High Five" }

        assertThat(highFiveBadge).isNotNull()
        assertThat(highFiveBadge!!.isUnlocked).isTrue()
    }

    @Test
    fun `assignAmountToGoal awards Priority Saver when priority goal exceeds 3 is completed`() {

        savingBalanceRepository.save(SavingBalance(userId = userId, unassignedAmount = BigDecimal("1000.00")))
        val highPriorityGoal = createAndSaveGoal(targetAmount = BigDecimal("500.00"), priority = 4)


        savingGoalService.assignAmountToGoal(
            highPriorityGoal.id,
            userId,
            AssignAmountRequest(amount = BigDecimal("500.00"))
        )


        val userAchievements = userAchievementRepository.findAllByUserId(userId, PageRequest.of(0, 10)).content
        val priorityBadge = userAchievements.find { it.achievement.titleEn == "Priority Saver" }

        assertThat(priorityBadge).isNotNull()
        assertThat(priorityBadge!!.isUnlocked).isTrue()
    }

    @Test
    fun `assignAmountToGoal does not award Priority Saver when completed goal priority is 3 or less`() {

        savingBalanceRepository.save(SavingBalance(userId = userId, unassignedAmount = BigDecimal("1000.00")))
        val lowPriorityGoal = createAndSaveGoal(targetAmount = BigDecimal("500.00"), priority = 2)


        savingGoalService.assignAmountToGoal(
            lowPriorityGoal.id,
            userId,
            AssignAmountRequest(amount = BigDecimal("500.00"))
        )


        val userAchievements = userAchievementRepository.findAllByUserId(userId, PageRequest.of(0, 10)).content
        val priorityBadge = userAchievements.find { it.achievement.titleEn == "Priority Saver" }

        assertThat(priorityBadge?.isUnlocked ?: false).isFalse()
    }


    private fun createAndSaveGoal(
        targetAmount: BigDecimal = BigDecimal("1000.00"),
        goalName: String = "Mobile",
        priority: Int = 2
    ): SavingGoal {
        return savingGoalRepository.save(
            SavingGoal(
                id = UUID.randomUUID(),
                userId = userId,
                goalName = goalName,
                targetAmount = targetAmount,
                deadline = LocalDateTime.now().plusMonths(3),
                goalIcon = GoalIcon.MOBILE,
                priority = priority,
                isCompleted = false
            )
        )
    }
}