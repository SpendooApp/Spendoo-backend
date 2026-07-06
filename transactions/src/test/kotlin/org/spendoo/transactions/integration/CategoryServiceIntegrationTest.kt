package org.spendoo.transactions.integration

import com.google.common.truth.Truth.assertThat
import com.ninjasquad.springmockk.MockkBean
import io.mockk.clearMocks
import io.mockk.every
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.spendoo.client.ApiClient
import org.spendoo.transactions.TransactionsTestApplication
import org.spendoo.transactions.api.dto.request.BudgetCreateRequest
import org.spendoo.transactions.api.dto.request.CategoryCreateRequest
import org.spendoo.transactions.api.dto.request.CategoryUpdateRequest
import org.spendoo.transactions.entity.*
import org.spendoo.transactions.repository.BudgetRepository
import org.spendoo.transactions.repository.CategoryRepository
import org.spendoo.transactions.repository.TransactionRepository
import org.spendoo.transactions.service.CategoryService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.domain.PageRequest
import org.springframework.test.context.ActiveProfiles
import java.math.BigDecimal
import java.time.Instant
import java.time.ZoneOffset
import java.util.*

@SpringBootTest(classes = [TransactionsTestApplication::class])
@ActiveProfiles("test")
class CategoryServiceIntegrationTest {

    @Autowired
    private lateinit var categoryService: CategoryService

    @Autowired
    private lateinit var categoryRepository: CategoryRepository

    @Autowired
    private lateinit var budgetRepository: BudgetRepository

    @Autowired
    private lateinit var transactionRepository: TransactionRepository

    private lateinit var existingUserId: UUID

    @MockkBean(relaxed = true)
    private lateinit var apiClient: ApiClient

    @BeforeEach
    fun setUp() {
        transactionRepository.deleteAll()
        budgetRepository.deleteAll()
        categoryRepository.deleteAll()
        existingUserId = UUID.randomUUID()
        clearMocks(apiClient)

        every { apiClient.call(any<Class<PlanCode>>(), any()) } returns PlanCode.FREE
    }

    @Test
    fun `create returns by saving category with zero amount active budget`() {
        val categoryCreateRequest = CategoryCreateRequest(
            categoryName = "Food",
            categoryIcon = CategoryIcon.FOOD,
            leftOverOptions = LeftOverOptions.MOVE_TO_NEXT_PERIOD,
            priority = 3,
            budget = BudgetCreateRequest(0.0, 30, Instant.now())
        )

        categoryService.create(categoryCreateRequest, existingUserId)

        val savedCategory = categoryRepository.findAll().single()
        val activeBudget = budgetRepository.findByCategoryIdAndIsActiveIsTrue(savedCategory.id)
        assertThat(savedCategory.categoryName).isEqualTo("Food")
        assertThat(savedCategory.userId).isEqualTo(existingUserId)
        assertThat(activeBudget).isNotNull()
        assertThat(activeBudget?.amount?.compareTo(BigDecimal.ZERO)).isEqualTo(0)
    }

    @Test
    fun `create returns by saving category and budget if request has budget`() {
        val categoryCreateRequest = CategoryCreateRequest(
            categoryName = "Utilities",
            categoryIcon = CategoryIcon.UTILITIES,
            leftOverOptions = LeftOverOptions.RESET_TO_ORIGINAL_AMOUNT,
            priority = 2,
            budget = BudgetCreateRequest(250.0, 30, Instant.now())
        )

        categoryService.create(categoryCreateRequest, existingUserId)

        val savedCategory = categoryRepository.findAll().single()
        val activeBudget = budgetRepository.findByCategoryIdAndIsActiveIsTrue(savedCategory.id)
        assertThat(savedCategory.categoryName).isEqualTo("Utilities")
        assertThat(activeBudget).isNotNull()
        assertThat(activeBudget?.amount?.compareTo(BigDecimal.valueOf(250.0))).isEqualTo(0)
    }

    @Test
    fun `getById returns category response if category exists`() {
        val savedCategory = createCategory(existingUserId, "Travel", budgetAmount = BigDecimal.valueOf(150.0))

        val categoryResponse = categoryService.getById(savedCategory.id, existingUserId)

        assertThat(categoryResponse.categoryId).isEqualTo(savedCategory.id)
        assertThat(categoryResponse.categoryName).isEqualTo("Travel")
    }

    @Test
    fun `getById returns zero spent amount when category budget has no spending`() {
        val savedCategory = createCategory(existingUserId, "Gym", withBudget = false)
        budgetRepository.save(
            Budget(
                amount = BigDecimal.valueOf(500.0),
                carryOver = BigDecimal.ZERO,
                period = 30,
                startDate = Instant.now().atZone(ZoneOffset.UTC).minusDays((2).toLong()).toInstant(),
                endDate = Instant.now().atZone(ZoneOffset.UTC).plusDays((28).toLong()).toInstant(),
                isActive = true,
                category = savedCategory
            )
        )

        val categoryResponse = categoryService.getById(savedCategory.id, existingUserId)

        assertThat(categoryResponse.budget.spentAmount).isEqualTo(BigDecimal.ZERO)
        assertThat(categoryResponse.budget.spendingPercentage).isEqualTo(0)
    }

    @Test
    fun `getById returns only in-range expense spending for active budget`() {
        val savedCategory = createCategory(existingUserId, "Bills", withBudget = false)
        val startDate = Instant.now().atZone(ZoneOffset.UTC).minusDays((5).toLong()).toInstant()
        val endDate = Instant.now().atZone(ZoneOffset.UTC).plusDays((5).toLong()).toInstant()
        budgetRepository.save(
            Budget(
                amount = BigDecimal.valueOf(1000.0),
                carryOver = BigDecimal.ZERO,
                period = 30,
                startDate = startDate,
                endDate = endDate,
                isActive = true,
                category = savedCategory
            )
        )
        transactionRepository.save(
            Transaction(
                userId = existingUserId,
                title = "In range expense",
                amount = BigDecimal.valueOf(-120.0),
                note = null,
                transactionDate = Instant.now(),
                category = savedCategory
            )
        )
        transactionRepository.save(
            Transaction(
                userId = existingUserId,
                title = "Out of range expense",
                amount = BigDecimal.valueOf(-300.0),
                note = null,
                transactionDate = Instant.now().atZone(ZoneOffset.UTC).minusDays((10).toLong()).toInstant(),
                category = savedCategory
            )
        )
        transactionRepository.save(
            Transaction(
                userId = existingUserId,
                title = "Income ignored",
                amount = BigDecimal.valueOf(900.0),
                note = null,
                transactionDate = Instant.now(),
                category = savedCategory
            )
        )

        val categoryResponse = categoryService.getById(savedCategory.id, existingUserId)

        assertThat(categoryResponse.budget.spentAmount.compareTo(BigDecimal.valueOf(-120.0))).isEqualTo(0)
    }

    @Test
    fun `getById throw IllegalArgumentException if category does not exist`() {
        val missingCategoryId = UUID.randomUUID()

        val thrownException = assertThrows<IllegalArgumentException> {
            categoryService.getById(missingCategoryId, existingUserId)
        }

        assertThat(thrownException).hasMessageThat().contains("Category not found")
    }

    @Test
    fun `getAll returns page with user categories if categories exist`() {
        createCategory(existingUserId, "Food")
        createCategory(existingUserId, "Shopping")

        val categoriesPage = categoryService.getAll(existingUserId, PageRequest.of(0, 10))

        assertThat(categoriesPage.totalElements).isEqualTo(2)
        assertThat(categoriesPage.content.map { it.categoryName }).containsAtLeast("Food", "Shopping")
    }

    @Test
    fun `getAll returns empty page if user has no categories`() {
        val otherUser = UUID.randomUUID()

        val categoriesPage = categoryService.getAll(otherUser, PageRequest.of(0, 10))

        assertThat(categoriesPage.totalElements).isEqualTo(0)
    }

    @Test
    fun `getAll excludes soft deleted categories`() {
        val activeCategory = createCategory(existingUserId, "Keep")
        val deletedCategory = createCategory(existingUserId, "Remove")
        categoryRepository.save(deletedCategory.copy(isDeleted = true))

        val categoriesPage = categoryService.getAll(existingUserId, PageRequest.of(0, 10))

        assertThat(categoriesPage.totalElements).isEqualTo(1)
        assertThat(categoriesPage.content.single().categoryId).isEqualTo(activeCategory.id)
    }

    @Test
    fun `update returns by updating category and active budget attributes`() {
        val savedCategory = createCategory(existingUserId, "Old Name")
        val categoryUpdateRequest = CategoryUpdateRequest(
            categoryName = "New Name",
            categoryIcon = CategoryIcon.CAR,
            leftOverOptions = LeftOverOptions.MOVE_TO_SAVINGS,
            priority = 1,
            budget = BudgetCreateRequest(0.0, 14, Instant.now())
        )

        categoryService.update(savedCategory.id, categoryUpdateRequest, existingUserId)

        val updatedCategory = categoryRepository.findByIdAndUserIdAndIsDeletedFalse(savedCategory.id, existingUserId)
        val updatedBudget = budgetRepository.findByCategoryIdAndIsActiveIsTrue(savedCategory.id)
        assertThat(updatedCategory?.categoryName).isEqualTo("New Name")
        assertThat(updatedCategory?.categoryIcon).isEqualTo(CategoryIcon.CAR)
        assertThat(updatedBudget?.amount?.compareTo(BigDecimal.ZERO)).isEqualTo(0)
        assertThat(updatedBudget?.period).isEqualTo(14)
    }

    @Test
    fun `update returns by updating category and budget if category exists and request has budget`() {
        val savedCategory = createCategory(existingUserId, "Rent", withBudget = false)
        budgetRepository.save(
            Budget(
                amount = BigDecimal.valueOf(400.0),
                carryOver = BigDecimal.ZERO,
                period = 30,
                startDate = Instant.now().atZone(ZoneOffset.UTC).minusDays((2).toLong()).toInstant(),
                endDate = Instant.now().atZone(ZoneOffset.UTC).plusDays((28).toLong()).toInstant(),
                isActive = true,
                category = savedCategory
            )
        )
        val categoryUpdateRequest = CategoryUpdateRequest(
            categoryName = "Rent Updated",
            categoryIcon = CategoryIcon.DEFAULT,
            leftOverOptions = LeftOverOptions.MOVE_TO_NEXT_PERIOD,
            priority = 2,
            budget = BudgetCreateRequest(600.0, 30, Instant.now())
        )

        categoryService.update(savedCategory.id, categoryUpdateRequest, existingUserId)

        val updatedCategory = categoryRepository.findByIdAndUserIdAndIsDeletedFalse(savedCategory.id, existingUserId)
        val updatedBudget = budgetRepository.findByCategoryIdAndIsActiveIsTrue(savedCategory.id)
        assertThat(updatedCategory?.categoryName).isEqualTo("Rent Updated")
        assertThat(updatedBudget?.amount?.compareTo(BigDecimal.valueOf(600.0))).isEqualTo(0)
    }

    @Test
    fun `update throw IllegalArgumentException if category does not exist`() {
        val missingCategoryId = UUID.randomUUID()
        val categoryUpdateRequest = CategoryUpdateRequest(
            categoryName = "No Category",
            categoryIcon = CategoryIcon.DEFAULT,
            leftOverOptions = LeftOverOptions.RESET_TO_ORIGINAL_AMOUNT,
            priority = 1,
            budget = BudgetCreateRequest(100.0, 30, Instant.now())
        )

        val thrownException = assertThrows<IllegalArgumentException> {
            categoryService.update(missingCategoryId, categoryUpdateRequest, existingUserId)
        }

        assertThat(thrownException).hasMessageThat().contains("Category not found")
    }

    @Test
    fun `delete returns by soft deleting category if category exists`() {
        val savedCategory = createCategory(existingUserId, "Delete Category")

        categoryService.delete(savedCategory.id, existingUserId)

        val activeCategory = categoryRepository.findByIdAndUserIdAndIsDeletedFalse(savedCategory.id, existingUserId)
        assertThat(activeCategory).isNull()
    }

    @Test
    fun `delete throw IllegalArgumentException if category does not exist`() {
        val missingCategoryId = UUID.randomUUID()

        val thrownException = assertThrows<IllegalArgumentException> {
            categoryService.delete(missingCategoryId, existingUserId)
        }

        assertThat(thrownException).hasMessageThat().contains("Category not found")
    }

    @Test
    fun `getSummary returns zero budget and null sums if user has no data`() {
        val summary = categoryService.getSummary(existingUserId)

        assertThat(summary.totalBudget?.compareTo(BigDecimal.ZERO)).isEqualTo(0)
        assertThat(summary.totalSpent).isNull()
        assertThat(summary.addedIncome).isNull()
    }

    @Test
    fun `getSummary returns aggregated budget spent and added income`() {
        val category = createCategory(existingUserId, "Summary Category", withBudget = false)
        val startDate = Instant.now().atZone(ZoneOffset.UTC).minusDays((2).toLong()).toInstant()
        val endDate = Instant.now().atZone(ZoneOffset.UTC).plusDays((2).toLong()).toInstant()

        budgetRepository.save(
            Budget(
                amount = BigDecimal.valueOf(500.0),
                carryOver = BigDecimal.ZERO,
                period = 30,
                startDate = startDate,
                endDate = endDate,
                isActive = true,
                category = category
            )
        )
        transactionRepository.save(
            Transaction(
                userId = existingUserId,
                title = "Expense in range",
                amount = BigDecimal.valueOf(-120.0),
                note = null,
                transactionDate = Instant.now(),
                category = category
            )
        )
        transactionRepository.save(
            Transaction(
                userId = existingUserId,
                title = "Added income",
                amount = BigDecimal.valueOf(300.0),
                note = null,
                transactionDate = Instant.now(),
                category = null
            )
        )

        val summary = categoryService.getSummary(existingUserId)

        assertThat(summary.totalBudget?.compareTo(BigDecimal.valueOf(500.0))).isEqualTo(0)
        assertThat(summary.totalSpent?.compareTo(BigDecimal.valueOf(-120.0))).isEqualTo(0)
        assertThat(summary.addedIncome?.compareTo(BigDecimal.valueOf(300.0))).isEqualTo(0)
    }


    private fun createCategory(
        userId: UUID,
        name: String,
        withBudget: Boolean = true,
        budgetAmount: BigDecimal = BigDecimal.ZERO
    ): Category {
        val category = categoryRepository.save(
            Category(
                userId = userId,
                categoryName = name,
                categoryIcon = CategoryIcon.DEFAULT,
                leftOverOptions = LeftOverOptions.RESET_TO_ORIGINAL_AMOUNT,
                priority = 2,
                isDeleted = false
            )
        )

        if (withBudget) {
            budgetRepository.save(
                Budget(
                    amount = budgetAmount,
                    carryOver = BigDecimal.ZERO,
                    period = 30,
                    startDate = Instant.now().atZone(ZoneOffset.UTC).minusDays((1).toLong()).toInstant(),
                    endDate = Instant.now().atZone(ZoneOffset.UTC).plusDays((29).toLong()).toInstant(),
                    isActive = true,
                    category = category
                )
            )
        }

        return category
    }

    @Test
    fun `getTopSpendingCategories returns categories with spending if expense data exists`() {
        val foodCategory = createCategory(existingUserId, "Food", withBudget = false)
        val transportCategory = createCategory(existingUserId, "Transport", withBudget = false)

        transactionRepository.save(
            Transaction(
                userId = existingUserId,
                title = "Food Expense",
                amount = BigDecimal.valueOf(-500.0),
                note = null,
                transactionDate = Instant.now(),
                category = foodCategory
            )
        )
        transactionRepository.save(
            Transaction(
                userId = existingUserId,
                title = "Transport Expense",
                amount = BigDecimal.valueOf(-200.0),
                note = null,
                transactionDate = Instant.now(),
                category = transportCategory
            )
        )
        val topSpendingPage = categoryService.getTopSpendingCategories(existingUserId, PageRequest.of(0, 10))

        assertThat(topSpendingPage.totalElements).isEqualTo(2)
        assertThat(topSpendingPage.content.map { it.categoryName }).containsExactly("Food", "Transport").inOrder()

        assertThat(topSpendingPage.content[0].id).isEqualTo(foodCategory.id)
        assertThat(topSpendingPage.content[1].id).isEqualTo(transportCategory.id)
    }

    @Test
    fun `getTopSpendingCategories returns empty page if user has no expense data`() {
        transactionRepository.save(
            Transaction(
                userId = existingUserId,
                title = "Salary",
                amount = BigDecimal.valueOf(3000.0),
                note = null,
                transactionDate = Instant.now(),
                category = null
            )
        )

        val topSpendingPage = categoryService.getTopSpendingCategories(existingUserId, PageRequest.of(0, 10))

        assertThat(topSpendingPage.totalElements).isEqualTo(0)
    }



}
