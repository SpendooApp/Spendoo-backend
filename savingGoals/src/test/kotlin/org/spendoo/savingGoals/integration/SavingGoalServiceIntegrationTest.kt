package org.spendoo.savingGoals.integration

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.spendoo.savingGoals.SavingGoalsTestApplication
import org.spendoo.savingGoals.api.dto.request.AssignAmountRequest
import org.spendoo.savingGoals.api.dto.request.GoalCreateRequest
import org.spendoo.savingGoals.api.dto.request.GoalUpdateRequest
import org.spendoo.savingGoals.entity.GoalIcon
import org.spendoo.savingGoals.entity.SavingBalance
import org.spendoo.savingGoals.entity.SavingGoal
import org.spendoo.savingGoals.repository.SavingBalanceRepository
import org.spendoo.savingGoals.repository.SavingGoalHistoryRepository
import org.spendoo.savingGoals.repository.SavingGoalRepository
import org.spendoo.savingGoals.service.SavingGoalService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.domain.PageRequest
import org.springframework.test.context.ActiveProfiles
import java.math.BigDecimal
import java.time.Instant
import java.time.ZoneOffset
import java.util.*

@SpringBootTest(classes = [SavingGoalsTestApplication::class])
@ActiveProfiles("test")
class SavingGoalServiceIntegrationTest {

    @Autowired
    private lateinit var savingGoalService: SavingGoalService

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
        savingGoalRepository.deleteAll()
        savingBalanceRepository.deleteAll()
    }

    @Test
    fun `createSavingGoal saves a new goal successfully`() {
        val request = GoalCreateRequest(
            goalName = "BMW",
            targetAmount = BigDecimal("120000000"),
            deadline = Instant.now().atZone(ZoneOffset.UTC).plusMonths(6).toInstant(),
            goalIcon = GoalIcon.CAR,
            priority = 4
        )

        savingGoalService.createSavingGoal(request, userId)

        val savedGoal = savingGoalRepository.findAll().single()
        assertThat(savedGoal.goalName).isEqualTo("BMW")
        assertThat(savedGoal.userId).isEqualTo(userId)

    }

    @Test
    fun `updateGoal updates specific fields when goal exists`() {
        val originalGoal = createAndSaveGoal(targetAmount = BigDecimal("10500.00"))
        val updateRequest = GoalUpdateRequest(
            goalName = "Updated Mobile Fund",
            targetAmount = BigDecimal("10600.00"),
            deadline = originalGoal.deadline,
            goalIcon = originalGoal.goalIcon,
            priority = originalGoal.priority
        )

        val updatedGoal = savingGoalService.updateGoal(originalGoal.id, updateRequest, userId)

        assertThat(updatedGoal.goalName).isEqualTo("Updated Mobile Fund")
        assertThat(updatedGoal.targetAmount).isEqualTo(BigDecimal("10600.00"))
        // Leftover fields retain their original state
        assertThat(updatedGoal.priority).isEqualTo(originalGoal.priority)
    }

    @Test
    fun `updateGoal throws IllegalArgumentException if goal does not exist`() {
        val missingGoalId = UUID.randomUUID()
        val updateRequest = GoalUpdateRequest(
            "PlayStation", BigDecimal("10600.00"), Instant.now().atZone(ZoneOffset.UTC).plusMonths(6).toInstant(), GoalIcon.ENTERTAINMENT, 2
        )

        val exception = assertThrows<IllegalArgumentException> {
            savingGoalService.updateGoal(missingGoalId, updateRequest, userId)
        }
        assertThat(exception.message).contains("Saving goal not found")
    }

    @Test
    fun `deleteGoal removes goal successfully `() {
        val goal = createAndSaveGoal()

        savingGoalService.deleteGoal(userId, goal.id)

        val deletedGoal = savingGoalRepository.findById(goal.id)
        assertThat(deletedGoal.isPresent).isFalse()
    }

    @Test
    fun `deleteGoal throw IllegalArgumentException if goal does not exist`() {
        val missingGoalId = UUID.randomUUID()

        val thrownException = assertThrows<IllegalArgumentException> {
            savingGoalService.deleteGoal(userId, missingGoalId)
        }

        assertThat(thrownException).hasMessageThat().contains("Saving goal not found")
    }

    @Test
    fun `getSavingGoalById returns returns correct goal details `() {

        val targetGoal = createAndSaveGoal()

        val response = savingGoalService.getSavingGoalById(targetGoal.id, userId)

        assertThat(response.goalId).isEqualTo(targetGoal.id)
        assertThat(response.goalName).isEqualTo("Apple iPhone 17 pro Max")
    }

    @Test
    fun `getAllGoals returns page with user goals if goals exist`() {
        createAndSaveGoal(targetAmount = BigDecimal("1000.00"), "Gift")
        createAndSaveGoal(targetAmount = BigDecimal("10000"), "PlayStation")

        val goalsPage = savingGoalService.getAllGoals(userId, PageRequest.of(0, 10))

        assertThat(goalsPage.totalElements).isEqualTo(2)
        assertThat(goalsPage.content.map { it.goalName }).containsAtLeast("Gift", "PlayStation")
    }

    @Test
    fun `addToSavings accurately increases the user unassigned balance`() {
        val depositAmount = BigDecimal("250.50")

        savingGoalService.addToSavings(userId, depositAmount)

        val currentUnassigned = savingBalanceRepository.getUnassignedAmount(userId)
        assertThat(currentUnassigned.compareTo(depositAmount)).isEqualTo(0)
    }

    @Test
    fun `assignAmountToGoal throws exception if unassigned balance is insufficient`() {
        val goal = createAndSaveGoal()
        val assignRequest = AssignAmountRequest(amount = BigDecimal("100.00"))

        val exception = assertThrows<IllegalArgumentException> {
            savingGoalService.assignAmountToGoal(goal.id, userId, assignRequest)
        }
        assertThat(exception.message).contains("Insufficient unassigned amount")
    }

    @Test
    fun `assignAmountToGoal successfully processes logic and marks goal completed if target is reached`() {
        // Give the user an unassigned balance to take from it
        savingBalanceRepository.save(SavingBalance(userId = userId, unassignedAmount = BigDecimal("1000.00")))

        val goal = createAndSaveGoal(targetAmount = BigDecimal("500.00"))
        val assignRequest = AssignAmountRequest(amount = BigDecimal("500.00"))

        savingGoalService.assignAmountToGoal(goal.id, userId, assignRequest)

        // Verify balance was deducted (1000 - 500 = 500)
        val remainingBalance = savingBalanceRepository.getUnassignedAmount(userId)
        assertThat(remainingBalance.compareTo(BigDecimal("500.00"))).isEqualTo(0)

        // Verify goal updated to completed state
        val updatedGoal = savingGoalRepository.findById(goal.id).get()
        assertThat(updatedGoal.isCompleted).isTrue()
    }

    @Test
    fun `getSummary calculates total targets and remaining funds perfectly`() {
        createAndSaveGoal(targetAmount = BigDecimal("300.00"))
        createAndSaveGoal(targetAmount = BigDecimal("700.00"))
        savingBalanceRepository.save(SavingBalance(userId = userId, unassignedAmount = BigDecimal("150.00")))

        val summary = savingGoalService.getSummary(userId)

        assertThat(summary.totalTarget.compareTo(BigDecimal("1000.00"))).isEqualTo(0)
        assertThat(summary.unassignedAmount.compareTo(BigDecimal("150.00"))).isEqualTo(0)
    }

    private fun createAndSaveGoal(
        targetAmount: BigDecimal = BigDecimal("1000.00"),
        goalName: String = "Apple iPhone 17 pro Max"
    ): SavingGoal {
        return savingGoalRepository.save(
            SavingGoal(
                id = UUID.randomUUID(),
                userId = userId,
                goalName = goalName,
                targetAmount = targetAmount,
                deadline = Instant.now().atZone(ZoneOffset.UTC).plusMonths(3).toInstant(),
                goalIcon = GoalIcon.MOBILE,
                priority = 2,
                isCompleted = false
            )
        )
    }
}