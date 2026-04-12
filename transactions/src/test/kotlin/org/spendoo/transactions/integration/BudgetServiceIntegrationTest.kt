package org.spendoo.transactions.integration

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.spendoo.transactions.TransactionsTestApplication
import org.spendoo.transactions.api.dto.request.BudgetCreateRequest
import org.spendoo.transactions.entity.Budget
import org.spendoo.transactions.entity.Category
import org.spendoo.transactions.entity.CategoryIcon
import org.spendoo.transactions.entity.LeftOverOptions
import org.spendoo.transactions.repository.BudgetRepository
import org.spendoo.transactions.repository.CategoryRepository
import org.spendoo.transactions.repository.TransactionRepository
import org.spendoo.transactions.service.BudgetService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import java.math.BigDecimal
import java.time.LocalDateTime
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

    @BeforeEach
    fun setUp() {
        transactionRepository.deleteAll()
        budgetRepository.deleteAll()
        categoryRepository.deleteAll()
    }

    @Test
    fun `createBudget returns saved budget if request is valid`() {
        val savedCategory = createCategory()
        val budgetCreateRequest =
            BudgetCreateRequest(amount = 1000.0, period = 30, startDate = LocalDateTime.now().minusDays(2))

        val createdBudget = budgetService.createBudget(budgetCreateRequest, savedCategory)

        assertThat(createdBudget.id).isNotNull()
        assertThat(createdBudget.category.id).isEqualTo(savedCategory.id)
        assertThat(createdBudget.amount).isEqualTo(BigDecimal.valueOf(1000.0))
        assertThat(createdBudget.isActive).isTrue()
    }

    @Test
    fun `updateBudget returns new budget if no active budget exists`() {
        val savedCategory = createCategory()
        val budgetCreateRequest = BudgetCreateRequest(amount = 300.0, period = 7, startDate = LocalDateTime.now())

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
                startDate = LocalDateTime.now().minusDays(2),
                endDate = LocalDateTime.now().plusDays(28),
                isActive = true,
                category = savedCategory
            )
        )
        val budgetCreateRequest = BudgetCreateRequest(amount = 250.0, period = 15, startDate = LocalDateTime.now())

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
                startDate = LocalDateTime.now().minusDays(40),
                endDate = LocalDateTime.now().minusDays(10),
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
                startDate = LocalDateTime.now().minusDays(40),
                endDate = LocalDateTime.now().minusDays(10),
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
                startDate = LocalDateTime.now().minusDays(40),
                endDate = LocalDateTime.now().minusDays(10),
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
    fun `deactivateBudgetForCategory returns by deactivating budget if active budget exists`() {
        val savedCategory = createCategory()
        val activeBudget = budgetRepository.save(
            Budget(
                amount = BigDecimal.valueOf(200.0),
                carryOver = BigDecimal.ZERO,
                period = 30,
                startDate = LocalDateTime.now().minusDays(3),
                endDate = LocalDateTime.now().plusDays(27),
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

    private fun createCategory(leftOverOptions: LeftOverOptions = LeftOverOptions.MOVE_TO_NEXT_PERIOD): Category {
        return categoryRepository.save(
            Category(
                userId = UUID.randomUUID(),
                categoryName = "Food",
                categoryIcon = CategoryIcon.FOOD,
                leftOverOptions = leftOverOptions,
                priority = 2,
                isDeleted = false
            )
        )
    }
}
