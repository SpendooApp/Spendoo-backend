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
import java.time.LocalDate
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

    @Autowired
    private lateinit var loginStreakRepository: LoginStreakRepository

    @Autowired
    private lateinit var transactionStreakRepository: TransactionStreakRepository

    private val userId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        loginStreakRepository.deleteAll()
        transactionStreakRepository.deleteAll()
        savingGoalHistoryRepository.deleteAll()
        userAchievementRepository.deleteAll()
        savingGoalRepository.deleteAll()
        savingBalanceRepository.deleteAll()
        achievementRepository.deleteAll()

        achievementService.ensureUserAchievementsCreated(userId)
    }

    @Test
    fun `addToSavings awards Savings L1 badge on first deposit`() {
        val depositAmount = BigDecimal(150.0)

        savingGoalService.addToSavings(userId, depositAmount)

        val userAchievements = userAchievementRepository.findAllByUserId(userId, PageRequest.of(0, 50)).content
        val l1Badge = userAchievements.find { it.achievement.code == AchievementCode.SAVINGS_L1 }

        assertThat(l1Badge).isNotNull()
        assertThat(l1Badge!!.isUnlocked).isTrue()
        assertThat(l1Badge.currentProgress.compareTo(BigDecimal(1.0))).isEqualTo(0)
    }

    @Test
    fun `addToSavings does not unlock duplicate Savings L1 badges on subsequent deposits`() {
        // Deposit twice
        savingGoalService.addToSavings(userId, BigDecimal(100.0))
        savingGoalService.addToSavings(userId, BigDecimal(200.0))

        // Ensure exactly one achievement entry exists
        val userAchievements = userAchievementRepository.findAllByUserId(userId, PageRequest.of(0, 50)).content
        val matches = userAchievements.filter { it.achievement.code == AchievementCode.SAVINGS_L1 }

        assertThat(matches).hasSize(1)
        assertThat(matches.first().isUnlocked).isTrue()
    }

    @Test
    fun `assignAmountToGoal awards Savings L4 badge when cumulative savings history reaches 10000`() {
        savingBalanceRepository.save(SavingBalance(userId = userId, unassignedAmount = BigDecimal(12000.0)))
        val goal = createAndSaveGoal(targetAmount = BigDecimal(10000.0), priority = 2)

        savingGoalService.assignAmountToGoal(goal.id, userId, AssignAmountRequest(amount = BigDecimal(10000.0)))

        val userAchievements = userAchievementRepository.findAllByUserId(userId, PageRequest.of(0, 50)).content
        val l4Badge = userAchievements.find { it.achievement.code == AchievementCode.SAVINGS_L4 }

        assertThat(l4Badge).isNotNull()
        assertThat(l4Badge!!.isUnlocked).isTrue()
        assertThat(l4Badge.currentProgress.compareTo(BigDecimal(10000.0))).isEqualTo(0)
    }

    @Test
    fun `assignAmountToGoal awards High Five badge when completing 5 goals`() {
        savingBalanceRepository.save(SavingBalance(userId = userId, unassignedAmount = BigDecimal(5000.0)))

        // Complete 4 goals
        for (i in 1..4) {
            val goal = createAndSaveGoal(targetAmount = BigDecimal(100.0), priority = 2)
            savingGoalService.assignAmountToGoal(goal.id, userId, AssignAmountRequest(amount = BigDecimal(100.0)))
        }

        var userAchievements = userAchievementRepository.findAllByUserId(userId, PageRequest.of(0, 50)).content
        var highFiveBadge = userAchievements.find { it.achievement.code == AchievementCode.HIGH_FIVE }
        assertThat(highFiveBadge?.isUnlocked ?: false).isFalse()

        // Complete the 5th goal
        val finalGoal = createAndSaveGoal(targetAmount = BigDecimal(100.0), priority = 2)
        savingGoalService.assignAmountToGoal(finalGoal.id, userId, AssignAmountRequest(amount = BigDecimal(100.0)))

        // High Five badge is unlocked dynamically
        userAchievements = userAchievementRepository.findAllByUserId(userId, PageRequest.of(0, 50)).content
        highFiveBadge = userAchievements.find { it.achievement.code == AchievementCode.HIGH_FIVE }

        assertThat(highFiveBadge).isNotNull()
        assertThat(highFiveBadge!!.isUnlocked).isTrue()
    }

    @Test
    fun `assignAmountToGoal awards Priority Saver when priority goal exceeds 3 is completed`() {
        savingBalanceRepository.save(SavingBalance(userId = userId, unassignedAmount = BigDecimal(1000.0)))
        val highPriorityGoal = createAndSaveGoal(targetAmount = BigDecimal(500.0), priority = 4)

        savingGoalService.assignAmountToGoal(
            highPriorityGoal.id,
            userId,
            AssignAmountRequest(amount = BigDecimal(500.0))
        )

        val userAchievements = userAchievementRepository.findAllByUserId(userId, PageRequest.of(0, 50)).content
        val priorityBadge = userAchievements.find { it.achievement.code == AchievementCode.PRIORITY_SAVER }

        assertThat(priorityBadge).isNotNull()
        assertThat(priorityBadge!!.isUnlocked).isTrue()
    }

    @Test
    fun `assignAmountToGoal does not award Priority Saver when completed goal priority is 3 or less`() {
        savingBalanceRepository.save(SavingBalance(userId = userId, unassignedAmount = BigDecimal(1000.0)))
        val lowPriorityGoal = createAndSaveGoal(targetAmount = BigDecimal(500.0), priority = 2)

        savingGoalService.assignAmountToGoal(
            lowPriorityGoal.id,
            userId,
            AssignAmountRequest(amount = BigDecimal(500.0))
        )

        val userAchievements = userAchievementRepository.findAllByUserId(userId, PageRequest.of(0, 50)).content
        val priorityBadge = userAchievements.find { it.achievement.code == AchievementCode.PRIORITY_SAVER }

        assertThat(priorityBadge?.isUnlocked ?: false).isFalse()
    }

    @Test
    fun `login streak increments and unlocks LOGIN_STREAK_L1 after 3 days`() {
        val yesterday = LocalDate.now().minusDays(1)
        val dayBefore = LocalDate.now().minusDays(2)

        // Seed yesterday and day before
        loginStreakRepository.save(LoginStreak(userId = userId, streakDate = dayBefore))
        loginStreakRepository.save(LoginStreak(userId = userId, streakDate = yesterday))

        achievementService.handleUserLoggedIn(userId)

        val userAchievements = userAchievementRepository.findAllByUserId(userId, PageRequest.of(0, 50)).content
        val streakBadge = userAchievements.find { it.achievement.code == AchievementCode.LOGIN_STREAK_L1 }

        assertThat(streakBadge).isNotNull()
        assertThat(streakBadge!!.isUnlocked).isTrue()
        assertThat(streakBadge.currentProgress.compareTo(BigDecimal(3.0))).isEqualTo(0)
    }

    @Test
    fun `transaction streak increments and unlocks TRANSACTION_STREAK_L1 after 3 days`() {
        val yesterday = LocalDate.now().minusDays(1)
        val dayBefore = LocalDate.now().minusDays(2)

        // Seed yesterday and day before
        transactionStreakRepository.save(TransactionStreak(userId = userId, streakDate = dayBefore))
        transactionStreakRepository.save(TransactionStreak(userId = userId, streakDate = yesterday))

        achievementService.handleTransactionCreated(userId, 1)

        val userAchievements = userAchievementRepository.findAllByUserId(userId, PageRequest.of(0, 50)).content
        val streakBadge = userAchievements.find { it.achievement.code == AchievementCode.TRANSACTION_STREAK_L1 }

        assertThat(streakBadge).isNotNull()
        assertThat(streakBadge!!.isUnlocked).isTrue()
        assertThat(streakBadge.currentProgress.compareTo(BigDecimal(3.0))).isEqualTo(0)
    }

    @Test
    fun `distinct categories count unlocks CATEG_L1 category explorer`() {
        achievementService.handleTransactionCreated(userId, 3)

        val userAchievements = userAchievementRepository.findAllByUserId(userId, PageRequest.of(0, 50)).content
        val categoryBadge = userAchievements.find { it.achievement.code == AchievementCode.CATEGORY_L1 }

        assertThat(categoryBadge).isNotNull()
        assertThat(categoryBadge!!.isUnlocked).isTrue()
    }

    private fun createAndSaveGoal(
        targetAmount: BigDecimal = BigDecimal(1000.0),
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