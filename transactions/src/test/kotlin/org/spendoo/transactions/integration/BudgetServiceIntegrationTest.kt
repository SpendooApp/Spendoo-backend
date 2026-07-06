package org.spendoo.transactions.integration

import com.google.common.truth.Truth.assertThat
import com.ninjasquad.springmockk.MockkBean
import io.mockk.clearMocks
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.spendoo.client.ApiClient
import org.spendoo.transactions.TransactionsTestApplication
import org.spendoo.transactions.api.dto.request.BudgetCreateRequest
import org.spendoo.transactions.entity.*
import org.spendoo.transactions.repository.BudgetRepository
import org.spendoo.transactions.repository.CategoryRepository
import org.spendoo.transactions.repository.TransactionRepository
import org.spendoo.transactions.service.BudgetService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import java.math.BigDecimal
import java.time.Instant
import java.time.ZoneOffset
import java.util.*

@SpringBootTest(classes = [TransactionsTestApplication::class])
@ActiveProfiles("test")
class BudgetServiceIntegrationTest {

    @Autowired
    private lateinit var budgetService: BudgetService

    @Autowired
    private lateinit var categoryRepository: CategoryRepository

    @Autowired
    private lateinit var budgetRepository: BudgetRepository

    @Autowired
    private lateinit var transactionRepository: TransactionRepository

    @MockkBean(relaxed = true)
    private lateinit var apiClient: ApiClient

    @BeforeEach
    fun setUp() {
        transactionRepository.deleteAll()
        budgetRepository.deleteAll()
        categoryRepository.deleteAll()
        clearMocks(apiClient)
    }

    @Test
    fun `createBudget returns saved budget if request is valid`() {
        val savedCategory = createCategory()
        val budgetCreateRequest =
            BudgetCreateRequest(amount = 1000.0, period = 30, startDate = Instant.now().atZone(ZoneOffset.UTC).minusDays((2).toLong()).toInstant())

        val createdBudget = budgetService.createBudget(budgetCreateRequest, savedCategory)

        assertThat(createdBudget.id).isNotNull()
        assertThat(createdBudget.category.id).isEqualTo(savedCategory.id)
        assertThat(createdBudget.amount).isEqualTo(BigDecimal.valueOf(1000.0))
        assertThat(createdBudget.isActive).isTrue()
    }

    @Test
    fun `updateBudget returns new budget if no active budget exists`() {
        val savedCategory = createCategory()
        val budgetCreateRequest = BudgetCreateRequest(amount = 300.0, period = 7, startDate = Instant.now())

        val updatedBudget = budgetService.updateBudget(budgetCreateRequest, savedCategory)

        assertThat(updatedBudget.category.id).isEqualTo(savedCategory.id)
        assertThat(updatedBudget.amount).isEqualTo(BigDecimal.valueOf(300.0))
        assertThat(updatedBudget.period).isEqualTo(7)
    }

    @Test
    fun `updateBudget returns updated budget if active budget exists`() {
        val savedCategory = createCategory()
        val existingBudget = budgetRepository.save(
            Budget(
                amount = BigDecimal.valueOf(120.0),
                carryOver = BigDecimal.ZERO,
                period = 30,
                startDate = Instant.now().atZone(ZoneOffset.UTC).minusDays((2).toLong()).toInstant(),
                endDate = Instant.now().atZone(ZoneOffset.UTC).plusDays((28).toLong()).toInstant(),
                isActive = true,
                category = savedCategory
            )
        )
        val budgetCreateRequest = BudgetCreateRequest(amount = 250.0, period = 15, startDate = Instant.now())

        val updatedBudget = budgetService.updateBudget(budgetCreateRequest, savedCategory)

        assertThat(updatedBudget.id).isEqualTo(existingBudget.id)
        assertThat(updatedBudget.amount).isEqualTo(BigDecimal.valueOf(250.0))
        assertThat(updatedBudget.period).isEqualTo(15)
        assertThat(updatedBudget.isActive).isTrue()
    }

    @Test
    fun `processExpiredBudget returns zero if no active budget exists`() {
        val savedCategory = createCategory()

        val carryOverResult = budgetService.processExpiredBudget(savedCategory.id)

        assertThat(carryOverResult).isEqualTo(BigDecimal.ZERO)
    }

    @Test
    fun `processExpiredBudget returns leftover if left over option is move to next period`() {
        val savedCategory = createCategory(leftOverOptions = LeftOverOptions.MOVE_TO_NEXT_PERIOD)
        budgetRepository.save(
            Budget(
                amount = BigDecimal.valueOf(500.0),
                carryOver = BigDecimal.ZERO,
                period = 30,
                startDate = Instant.now().atZone(ZoneOffset.UTC).minusDays((40).toLong()).toInstant(),
                endDate = Instant.now().atZone(ZoneOffset.UTC).minusDays((10).toLong()).toInstant(),
                isActive = true,
                category = savedCategory
            )
        )

        val carryOverResult = budgetService.processExpiredBudget(savedCategory.id)

        val deactivatedBudget = budgetRepository.findByCategoryIdAndIsActiveIsTrue(savedCategory.id)
        assertThat(carryOverResult.compareTo(BigDecimal.valueOf(500.0))).isEqualTo(0)
        assertThat(deactivatedBudget).isNull()
    }

    @Test
    fun `processExpiredBudget returns zero if left over option is reset to original amount`() {
        val savedCategory = createCategory(leftOverOptions = LeftOverOptions.RESET_TO_ORIGINAL_AMOUNT)
        budgetRepository.save(
            Budget(
                amount = BigDecimal.valueOf(450.0),
                carryOver = BigDecimal.ZERO,
                period = 30,
                startDate = Instant.now().atZone(ZoneOffset.UTC).minusDays((40).toLong()).toInstant(),
                endDate = Instant.now().atZone(ZoneOffset.UTC).minusDays((10).toLong()).toInstant(),
                isActive = true,
                category = savedCategory
            )
        )

        val carryOverResult = budgetService.processExpiredBudget(savedCategory.id)

        assertThat(carryOverResult).isEqualTo(BigDecimal.ZERO)
    }

    @Test
    fun `processExpiredBudget returns leftover if left over option is move to savings`() {
        val savedCategory = createCategory(leftOverOptions = LeftOverOptions.MOVE_TO_SAVINGS)
        budgetRepository.save(
            Budget(
                amount = BigDecimal.valueOf(350.0),
                carryOver = BigDecimal.ZERO,
                period = 30,
                startDate = Instant.now().atZone(ZoneOffset.UTC).minusDays((40).toLong()).toInstant(),
                endDate = Instant.now().atZone(ZoneOffset.UTC).minusDays((10).toLong()).toInstant(),
                isActive = true,
                category = savedCategory
            )
        )

        val carryOverResult = budgetService.processExpiredBudget(savedCategory.id)

        assertThat(carryOverResult.compareTo(BigDecimal.valueOf(350.0))).isEqualTo(0)
    }

    @Test
    fun `calculateSpentAmount returns zero if no expense transactions exist for category`() {
        val savedCategory = createCategory()

        val spentAmount = budgetService.calculateSpentAmount(savedCategory.userId, savedCategory.id)

        assertThat(spentAmount).isEqualTo(BigDecimal.ZERO)
    }

    @Test
    fun `calculateSpentAmount returns only expense sum for requested user and category`() {
        val userId = UUID.randomUUID()
        val targetCategory = createCategory(userId = userId)
        val anotherCategory = createCategory(userId = userId)
        val otherUserCategory = createCategory(userId = UUID.randomUUID())

        transactionRepository.save(
            Transaction(
                userId = userId,
                title = "Target expense A",
                amount = BigDecimal.valueOf(-120.0),
                note = null,
                transactionDate = Instant.now(),
                category = targetCategory
            )
        )
        transactionRepository.save(
            Transaction(
                userId = userId,
                title = "Target expense B",
                amount = BigDecimal.valueOf(-30.0),
                note = null,
                transactionDate = Instant.now(),
                category = targetCategory
            )
        )
        transactionRepository.save(
            Transaction(
                userId = userId,
                title = "Target income ignored",
                amount = BigDecimal.valueOf(999.0),
                note = null,
                transactionDate = Instant.now(),
                category = targetCategory
            )
        )
        transactionRepository.save(
            Transaction(
                userId = userId,
                title = "Other category ignored",
                amount = BigDecimal.valueOf(-400.0),
                note = null,
                transactionDate = Instant.now(),
                category = anotherCategory
            )
        )
        transactionRepository.save(
            Transaction(
                userId = UUID.randomUUID(),
                title = "Other user ignored",
                amount = BigDecimal.valueOf(-700.0),
                note = null,
                transactionDate = Instant.now(),
                category = otherUserCategory
            )
        )

        val spentAmount = budgetService.calculateSpentAmount(userId, targetCategory.id)

        assertThat(spentAmount.compareTo(BigDecimal.valueOf(-150.0))).isEqualTo(0)
    }

    @Test
    fun `calculateSpentAmount returns zero if expenses exist only in other categories`() {
        val userId = UUID.randomUUID()
        val targetCategory = createCategory(userId = userId)
        val anotherCategory = createCategory(userId = userId)
        transactionRepository.save(
            Transaction(
                userId = userId,
                title = "Other category expense",
                amount = BigDecimal.valueOf(-80.0),
                note = null,
                transactionDate = Instant.now(),
                category = anotherCategory
            )
        )

        val spentAmount = budgetService.calculateSpentAmount(userId, targetCategory.id)

        assertThat(spentAmount).isEqualTo(BigDecimal.ZERO)
    }

    @Test
    fun `deactivateBudgetForCategory returns by deactivating budget if active budget exists`() {
        val savedCategory = createCategory()
        val activeBudget = budgetRepository.save(
            Budget(
                amount = BigDecimal.valueOf(200.0),
                carryOver = BigDecimal.ZERO,
                period = 30,
                startDate = Instant.now().atZone(ZoneOffset.UTC).minusDays((3).toLong()).toInstant(),
                endDate = Instant.now().atZone(ZoneOffset.UTC).plusDays((27).toLong()).toInstant(),
                isActive = true,
                category = savedCategory
            )
        )

        budgetService.deactivateBudgetForCategory(savedCategory.id)

        val persistedBudget = budgetRepository.findById(activeBudget.id).orElseThrow()
        assertThat(persistedBudget.isActive).isFalse()
    }

    @Test
    fun `deactivateBudgetForCategory returns without changes if active budget does not exist`() {
        val randomCategoryId = UUID.randomUUID()

        budgetService.deactivateBudgetForCategory(randomCategoryId)

        assertThat(budgetRepository.count()).isEqualTo(0)
    }

    @Test
    fun `moveToSavings triggers apiClient call when amount is positive`() {
        val userId = UUID.randomUUID()
        val positiveAmount = BigDecimal.valueOf(100.0)
        budgetService.moveToSavings(userId, positiveAmount)
        verify(exactly = 1) {
            apiClient.call(Unit::class.java, any())
        }
    }

    private fun createCategory(
        leftOverOptions: LeftOverOptions = LeftOverOptions.MOVE_TO_NEXT_PERIOD,
        userId: UUID = UUID.randomUUID()
    ): Category {
        return categoryRepository.save(
            Category(
                userId = userId,
                categoryName = "Food",
                categoryIcon = CategoryIcon.FOOD,
                leftOverOptions = leftOverOptions,
                priority = 2,
                isDeleted = false
            )
        )
    }
}
