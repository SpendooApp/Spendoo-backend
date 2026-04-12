package org.spendoo.transactions.integration

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.spendoo.transactions.TransactionsTestApplication
import org.spendoo.transactions.api.dto.request.BudgetCreateRequest
import org.spendoo.transactions.api.dto.request.CategoryCreateRequest
import org.spendoo.transactions.api.dto.request.CategoryUpdateRequest
import org.spendoo.transactions.entity.Budget
import org.spendoo.transactions.entity.Category
import org.spendoo.transactions.entity.CategoryIcon
import org.spendoo.transactions.entity.LeftOverOptions
import org.spendoo.transactions.repository.BudgetRepository
import org.spendoo.transactions.repository.CategoryRepository
import org.spendoo.transactions.repository.TransactionRepository
import org.spendoo.transactions.service.CategoryService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.domain.PageRequest
import org.springframework.test.context.ActiveProfiles
import java.math.BigDecimal
import java.time.LocalDateTime
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

    @BeforeEach
    fun setUp() {
        transactionRepository.deleteAll()
        budgetRepository.deleteAll()
        categoryRepository.deleteAll()
        existingUserId = UUID.randomUUID()
    }

    @Test
    fun `create returns by saving category if request has no budget`() {
        val categoryCreateRequest = CategoryCreateRequest(
            categoryName = "Food",
            categoryIcon = CategoryIcon.FOOD,
            leftOverOptions = LeftOverOptions.MOVE_TO_NEXT_PERIOD,
            priority = 3,
            budget = null
        )

        categoryService.create(categoryCreateRequest, existingUserId)

        val savedCategory = categoryRepository.findAll().single()
        assertThat(savedCategory.categoryName).isEqualTo("Food")
        assertThat(savedCategory.userId).isEqualTo(existingUserId)
    }

    @Test
    fun `create returns by saving category and budget if request has budget`() {
        val categoryCreateRequest = CategoryCreateRequest(
            categoryName = "Utilities",
            categoryIcon = CategoryIcon.UTILITIES,
            leftOverOptions = LeftOverOptions.RESET_TO_ORIGINAL_AMOUNT,
            priority = 2,
            budget = BudgetCreateRequest(250.0, 30, LocalDateTime.now())
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
        val savedCategory = createCategory(existingUserId, "Travel")

        val categoryResponse = categoryService.getById(savedCategory.id, existingUserId)

        assertThat(categoryResponse.categoryId).isEqualTo(savedCategory.id)
        assertThat(categoryResponse.categoryName).isEqualTo("Travel")
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
    fun `update returns by updating category if category exists and request has no budget`() {
        val savedCategory = createCategory(existingUserId, "Old Name")
        val categoryUpdateRequest = CategoryUpdateRequest(
            categoryName = "New Name",
            categoryIcon = CategoryIcon.CAR,
            leftOverOptions = LeftOverOptions.MOVE_TO_SAVINGS,
            priority = 1,
            budget = null
        )

        categoryService.update(savedCategory.id, categoryUpdateRequest, existingUserId)

        val updatedCategory = categoryRepository.findByIdAndUserIdAndIsDeletedFalse(savedCategory.id, existingUserId)
        assertThat(updatedCategory?.categoryName).isEqualTo("New Name")
        assertThat(updatedCategory?.categoryIcon).isEqualTo(CategoryIcon.CAR)
    }

    @Test
    fun `update returns by updating category and budget if category exists and request has budget`() {
        val savedCategory = createCategory(existingUserId, "Rent")
        budgetRepository.save(
            Budget(
                amount = BigDecimal.valueOf(400.0),
                carryOver = BigDecimal.ZERO,
                period = 30,
                startDate = LocalDateTime.now().minusDays(2),
                endDate = LocalDateTime.now().plusDays(28),
                isActive = true,
                category = savedCategory
            )
        )
        val categoryUpdateRequest = CategoryUpdateRequest(
            categoryName = "Rent Updated",
            categoryIcon = CategoryIcon.DEFAULT,
            leftOverOptions = LeftOverOptions.MOVE_TO_NEXT_PERIOD,
            priority = 2,
            budget = BudgetCreateRequest(600.0, 30, LocalDateTime.now())
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
            budget = null
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


    private fun createCategory(userId: UUID, name: String): Category {
        return categoryRepository.save(
            Category(
                userId = userId,
                categoryName = name,
                categoryIcon = CategoryIcon.DEFAULT,
                leftOverOptions = LeftOverOptions.RESET_TO_ORIGINAL_AMOUNT,
                priority = 2,
                isDeleted = false
            )
        )
    }
}
