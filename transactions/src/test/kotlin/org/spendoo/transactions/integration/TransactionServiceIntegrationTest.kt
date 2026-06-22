package org.spendoo.transactions.integration

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.spendoo.transactions.TransactionsTestApplication
import org.spendoo.transactions.api.dto.request.*
import org.spendoo.transactions.entity.*
import org.spendoo.transactions.repository.BudgetRepository
import org.spendoo.transactions.repository.CategoryRepository
import org.spendoo.transactions.repository.ScheduledPaymentRepository
import org.spendoo.transactions.repository.TransactionRepository
import org.spendoo.transactions.service.TransactionService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.test.context.ActiveProfiles
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.*

@SpringBootTest(classes = [TransactionsTestApplication::class])
@ActiveProfiles("test")
class TransactionServiceIntegrationTest {

    @Autowired
    private lateinit var transactionService: TransactionService

    @Autowired
    private lateinit var categoryRepository: CategoryRepository

    @Autowired
    private lateinit var scheduledPaymentRepository: ScheduledPaymentRepository

    @Autowired
    private lateinit var budgetRepository: BudgetRepository

    @Autowired
    private lateinit var transactionRepository: TransactionRepository

    private lateinit var existingUserId: UUID
    private lateinit var existingCategory: Category

    @BeforeEach
    fun setUp() {
        transactionRepository.deleteAll()
        scheduledPaymentRepository.deleteAll()
        budgetRepository.deleteAll()
        categoryRepository.deleteAll()
        existingUserId = UUID.randomUUID()
        existingCategory = createCategory(existingUserId, "Food")
    }

    @Test
    fun `createExpenseTransactions returns by saving expense transactions if categories exist`() {
        val createExpenseTransactionRequest = CreateExpenseTransactionRequest(
            entries = listOf(
                ExpenseTransactionEntryDto(
                    title = "Lunch",
                    amount = BigDecimal.valueOf(50.0),
                    categoryId = existingCategory.id,
                    transactionDate = LocalDateTime.now(),
                    note = "work day"
                )
            )
        )

        transactionService.createExpenseTransactions(existingUserId, createExpenseTransactionRequest)

        val savedTransactions = transactionRepository.findAllByUserId(existingUserId, PageRequest.of(0, 10))
        assertThat(savedTransactions.totalElements).isEqualTo(1)
        assertThat(savedTransactions.content.first().amount.compareTo(BigDecimal.valueOf(-50.0))).isEqualTo(0)
    }

    @Test
    fun `createExpenseTransactions throw IllegalArgumentException if category does not exist`() {
        val createExpenseTransactionRequest = CreateExpenseTransactionRequest(
            entries = listOf(
                ExpenseTransactionEntryDto(
                    title = "Taxi",
                    amount = BigDecimal.valueOf(30.0),
                    categoryId = UUID.randomUUID(),
                    transactionDate = LocalDateTime.now(),
                    note = null
                )
            )
        )

        val thrownException = assertThrows<IllegalArgumentException> {
            transactionService.createExpenseTransactions(existingUserId, createExpenseTransactionRequest)
        }

        assertThat(thrownException).hasMessageThat().contains("Category not found with ID")
    }

    @Test
    fun `createExpenseTransactions throw IllegalArgumentException if category is soft deleted`() {
        categoryRepository.save(existingCategory.copy(isDeleted = true))
        val createExpenseTransactionRequest = CreateExpenseTransactionRequest(
            entries = listOf(
                ExpenseTransactionEntryDto(
                    title = "Taxi",
                    amount = BigDecimal.valueOf(30.0),
                    categoryId = existingCategory.id,
                    transactionDate = LocalDateTime.now(),
                    note = null
                )
            )
        )

        val thrownException = assertThrows<IllegalArgumentException> {
            transactionService.createExpenseTransactions(existingUserId, createExpenseTransactionRequest)
        }

        assertThat(thrownException).hasMessageThat().contains("Category not found with ID")
    }

    @Test
    fun `createIncomeTransactions returns by saving income transactions if request is valid`() {
        val createIncomeTransactionRequest = CreateIncomeTransactionRequest(
            entries = listOf(
                IncomeTransactionEntryDto(
                    title = "Salary",
                    amount = BigDecimal.valueOf(5000.0),
                    transactionDate = LocalDateTime.now(),
                    note = "monthly"
                )
            )
        )

        transactionService.createIncomeTransactions(existingUserId, createIncomeTransactionRequest)

        val savedTransactions = transactionRepository.findAllByUserId(existingUserId, PageRequest.of(0, 10))
        assertThat(savedTransactions.totalElements).isEqualTo(1)
        assertThat(savedTransactions.content.first().amount.compareTo(BigDecimal.valueOf(5000.0))).isEqualTo(0)
        assertThat(savedTransactions.content.first().category).isNull()
    }

    @Test
    fun `updateTransaction returns by updating expense transaction if request has valid category`() {
        val existingTransaction =
            createExpenseTransaction(existingUserId, existingCategory, BigDecimal.valueOf(-120.0))
        val transactionUpdateRequest = TransactionUpdateRequest(
            title = "Updated Grocery",
            transactionDate = LocalDateTime.now(),
            note = "new note",
            amount = BigDecimal.valueOf(140.0),
            categoryId = existingCategory.id
        )

        transactionService.updateTransaction(existingTransaction.id, existingUserId, transactionUpdateRequest)

        val updatedTransaction = transactionRepository.findByIdAndUserId(existingTransaction.id, existingUserId)
        assertThat(updatedTransaction?.title).isEqualTo("Updated Grocery")
        assertThat(updatedTransaction?.amount?.compareTo(BigDecimal.valueOf(-140.0))).isEqualTo(0)
    }

    @Test
    fun `updateTransaction throw IllegalArgumentException if transaction does not exist`() {
        val transactionUpdateRequest = TransactionUpdateRequest(
            title = "Missing",
            transactionDate = LocalDateTime.now(),
            note = null,
            amount = BigDecimal.valueOf(70.0),
            categoryId = existingCategory.id
        )

        val thrownException = assertThrows<IllegalArgumentException> {
            transactionService.updateTransaction(UUID.randomUUID(), existingUserId, transactionUpdateRequest)
        }

        assertThat(thrownException).hasMessageThat().contains("Transaction not found")
    }

    @Test
    fun `updateTransaction throw IllegalArgumentException if transaction is expense and category is null`() {
        val existingTransaction = createExpenseTransaction(existingUserId, existingCategory, BigDecimal.valueOf(-90.0))
        val transactionUpdateRequest = TransactionUpdateRequest(
            title = "Missing Category",
            transactionDate = LocalDateTime.now(),
            note = null,
            amount = BigDecimal.valueOf(90.0),
            categoryId = null
        )

        val thrownException = assertThrows<IllegalArgumentException> {
            transactionService.updateTransaction(existingTransaction.id, existingUserId, transactionUpdateRequest)
        }

        assertThat(thrownException).hasMessageThat().contains("Expense transactions must have a category")
    }

    @Test
    fun `updateTransaction throw IllegalArgumentException if request category does not exist`() {
        val existingTransaction = createExpenseTransaction(existingUserId, existingCategory, BigDecimal.valueOf(-90.0))
        val transactionUpdateRequest = TransactionUpdateRequest(
            title = "Missing Category Id",
            transactionDate = LocalDateTime.now(),
            note = null,
            amount = BigDecimal.valueOf(90.0),
            categoryId = UUID.randomUUID()
        )

        val thrownException = assertThrows<IllegalArgumentException> {
            transactionService.updateTransaction(existingTransaction.id, existingUserId, transactionUpdateRequest)
        }

        assertThat(thrownException).hasMessageThat().contains("Category not found with ID")
    }

    @Test
    fun `updateTransaction throw IllegalArgumentException if existing transaction is income and request category is provided`() {
        val existingTransaction = createIncomeTransaction(existingUserId, BigDecimal.valueOf(400.0))
        val transactionUpdateRequest = TransactionUpdateRequest(
            title = "Income cannot become expense",
            transactionDate = LocalDateTime.now(),
            note = null,
            amount = BigDecimal.valueOf(400.0),
            categoryId = existingCategory.id
        )

        val thrownException = assertThrows<IllegalArgumentException> {
            transactionService.updateTransaction(existingTransaction.id, existingUserId, transactionUpdateRequest)
        }

        assertThat(thrownException).hasMessageThat().contains("Income transactions cannot have a category")
    }

    @Test
    fun `getTransactionById returns transaction if transaction exists`() {
        val existingTransaction = createIncomeTransaction(existingUserId, BigDecimal.valueOf(1100.0))

        val foundTransaction = transactionService.getTransactionById(existingTransaction.id, existingUserId)

        assertThat(foundTransaction.id).isEqualTo(existingTransaction.id)
        assertThat(foundTransaction.amount.compareTo(BigDecimal.valueOf(1100.0))).isEqualTo(0)
    }

    @Test
    fun `getTransactionById throw IllegalArgumentException if transaction does not exist`() {
        val missingTransactionId = UUID.randomUUID()

        val thrownException = assertThrows<IllegalArgumentException> {
            transactionService.getTransactionById(missingTransactionId, existingUserId)
        }

        assertThat(thrownException).hasMessageThat().contains("Transaction not found")
    }

    @Test
    fun `getTransactionsByDateRange returns matching transactions if range includes data`() {
        val oldTransactionDate = LocalDateTime.now().minusDays(10)
        val newTransactionDate = LocalDateTime.now().minusDays(2)
        transactionRepository.save(
            Transaction(
                userId = existingUserId,
                title = "Old",
                amount = BigDecimal.valueOf(10.0),
                note = null,
                transactionDate = oldTransactionDate,
                category = null
            )
        )
        transactionRepository.save(
            Transaction(
                userId = existingUserId,
                title = "New",
                amount = BigDecimal.valueOf(20.0),
                note = null,
                transactionDate = newTransactionDate,
                category = null
            )
        )

        val transactionsPage = transactionService.getTransactionsByDateRange(
            existingUserId,
            LocalDateTime.now().minusDays(5),
            LocalDateTime.now(),
            PageRequest.of(0, 10)
        )

        assertThat(transactionsPage.totalElements).isEqualTo(1)
        assertThat(transactionsPage.content.first().title).isEqualTo("New")
    }

    @Test
    fun `getTransactionsByDateRange includes start and end boundaries`() {
        val end = LocalDateTime.of(2026, 1, 20, 10, 0, 0)
        val start = end.minusDays(2)
        transactionRepository.save(
            Transaction(
                userId = existingUserId,
                title = "At start",
                amount = BigDecimal.valueOf(10.0),
                note = null,
                transactionDate = start,
                category = null
            )
        )
        transactionRepository.save(
            Transaction(
                userId = existingUserId,
                title = "At end",
                amount = BigDecimal.valueOf(20.0),
                note = null,
                transactionDate = end,
                category = null
            )
        )

        val transactionsPage = transactionService.getTransactionsByDateRange(
            existingUserId,
            start,
            end,
            PageRequest.of(0, 10)
        )

        assertThat(transactionsPage.totalElements).isEqualTo(2)
        assertThat(transactionsPage.content.map { it.title }).containsAtLeast("At start", "At end")
    }

    @Test
    fun `getTransactionsByDateRange excludes transactions from other users`() {
        val otherUserId = UUID.randomUUID()
        val now = LocalDateTime.now()
        transactionRepository.save(
            Transaction(
                userId = otherUserId,
                title = "Other user tx",
                amount = BigDecimal.valueOf(99.0),
                note = null,
                transactionDate = now,
                category = null
            )
        )

        val transactionsPage = transactionService.getTransactionsByDateRange(
            existingUserId,
            now.minusDays(1),
            now.plusDays(1),
            PageRequest.of(0, 10)
        )

        assertThat(transactionsPage.totalElements).isEqualTo(0)
    }

    @Test
    fun `getAll returns user transactions if user has transactions`() {
        createIncomeTransaction(existingUserId, BigDecimal.valueOf(2000.0))
        createIncomeTransaction(existingUserId, BigDecimal.valueOf(3000.0))

        val transactionsPage = transactionService.getAll(existingUserId, null, PageRequest.of(0, 10))

        assertThat(transactionsPage.totalElements).isEqualTo(2)
    }

    @Test
    fun `getAll excludes budgets with zero amount`() {
        budgetRepository.save(
            Budget(
                amount = BigDecimal.valueOf(1000.0),
                carryOver = BigDecimal.ZERO,
                period = 30,
                startDate = LocalDateTime.now().minusDays(1),
                endDate = LocalDateTime.now().plusDays(29),
                isActive = true,
                category = existingCategory
            )
        )
        budgetRepository.save(
            Budget(
                amount = BigDecimal.ZERO,
                carryOver = BigDecimal.ZERO,
                period = 30,
                startDate = LocalDateTime.now().minusDays(1),
                endDate = LocalDateTime.now().plusDays(29),
                isActive = true,
                category = existingCategory
            )
        )
        createIncomeTransaction(existingUserId, BigDecimal.valueOf(2000.0))

        val transactionsPage = transactionService.getAll(existingUserId, null, PageRequest.of(0, 10))

        assertThat(transactionsPage.totalElements).isEqualTo(2)
        val amounts = transactionsPage.content.map { it.amount.stripTrailingZeros() }
        val expectedAmounts = listOf(BigDecimal.valueOf(2000).stripTrailingZeros(), BigDecimal.valueOf(1000).stripTrailingZeros())
        assertThat(amounts).containsExactlyElementsIn(expectedAmounts)
    }

    @Test
    fun `getAll supports sorting by transactionDate field`() {
        transactionRepository.save(
            Transaction(
                userId = existingUserId,
                title = "First",
                amount = BigDecimal.valueOf(10.0),
                note = null,
                transactionDate = LocalDateTime.now().minusDays(5),
                category = null
            )
        )
        transactionRepository.save(
            Transaction(
                userId = existingUserId,
                title = "Second",
                amount = BigDecimal.valueOf(20.0),
                note = null,
                transactionDate = LocalDateTime.now().minusDays(2),
                category = null
            )
        )

        val sort = Sort.by(Sort.Order(Sort.Direction.DESC, "transactionDate"))
        val transactionsPage = transactionService.getAll(existingUserId, null, PageRequest.of(0, 10, sort))

        assertThat(transactionsPage.totalElements).isEqualTo(2)
        assertThat(transactionsPage.content[0].title).isEqualTo("Second")
        assertThat(transactionsPage.content[1].title).isEqualTo("First")
    }

    @Test
    fun `deleteTransaction returns by deleting transaction if transaction exists`() {
        val existingTransaction = createIncomeTransaction(existingUserId, BigDecimal.valueOf(1900.0))

        transactionService.deleteTransaction(existingUserId, existingTransaction.id)

        val deletedTransaction = transactionRepository.findByIdAndUserId(existingTransaction.id, existingUserId)
        assertThat(deletedTransaction).isNull()
    }

    @Test
    fun `deleteTransaction throw IllegalArgumentException if transaction does not exist`() {
        val missingTransactionId = UUID.randomUUID()

        val thrownException = assertThrows<IllegalArgumentException> {
            transactionService.deleteTransaction(existingUserId, missingTransactionId)
        }

        assertThat(thrownException).hasMessageThat().contains("Transaction not found")
    }

    @Test
    fun `getBalanceSummary returns zeroed summary if user has no data`() {
        val otherUser = UUID.randomUUID()

        val balanceSummary = runBlocking { transactionService.getBalanceSummary(otherUser) }

        assertThat(balanceSummary.totalBalance).isEqualTo(BigDecimal.ZERO)
        assertThat(balanceSummary.income).isEqualTo(BigDecimal.ZERO)
        assertThat(balanceSummary.expenses).isEqualTo(BigDecimal.ZERO)
    }

    @Test
    fun `getBalanceSummary returns calculated summary if user has budgets income and expenses`() {
        budgetRepository.save(
            Budget(
                amount = BigDecimal.valueOf(1000.0),
                carryOver = BigDecimal.ZERO,
                period = 30,
                startDate = LocalDateTime.now().minusDays(1),
                endDate = LocalDateTime.now().plusDays(29),
                isActive = true,
                category = existingCategory
            )
        )
        createIncomeTransaction(existingUserId, BigDecimal.valueOf(2000.0))
        createExpenseTransaction(existingUserId, existingCategory, BigDecimal.valueOf(-300.0))

        val balanceSummary = runBlocking { transactionService.getBalanceSummary(existingUserId) }

        assertThat(balanceSummary.income.compareTo(BigDecimal.valueOf(3000.0))).isEqualTo(0)
        assertThat(balanceSummary.expenses.compareTo(BigDecimal.valueOf(300.0))).isEqualTo(0)
        assertThat(balanceSummary.totalBalance.compareTo(BigDecimal.valueOf(2700.0))).isEqualTo(0)
    }

    @Test
    fun `getBalanceSummary returns zero income and positive expenses if user has only expenses`() {
        createExpenseTransaction(existingUserId, existingCategory, BigDecimal.valueOf(-120.0))

        val balanceSummary = runBlocking { transactionService.getBalanceSummary(existingUserId) }

        assertThat(balanceSummary.income).isEqualTo(BigDecimal.ZERO)
        assertThat(balanceSummary.expenses.compareTo(BigDecimal.valueOf(120.0))).isEqualTo(0)
        assertThat(balanceSummary.totalBalance.compareTo(BigDecimal.valueOf(-120.0))).isEqualTo(0)
    }




    private fun createCategory(userId: UUID, categoryName: String): Category {
        return categoryRepository.save(
            Category(
                userId = userId,
                categoryName = categoryName,
                categoryIcon = CategoryIcon.DEFAULT,
                leftOverOptions = LeftOverOptions.RESET_TO_ORIGINAL_AMOUNT,
                priority = 2,
                isDeleted = false
            )
        )
    }

    private fun createIncomeTransaction(userId: UUID, amount: BigDecimal): Transaction {
        return transactionRepository.save(
            Transaction(
                userId = userId,
                title = "Income",
                amount = amount,
                note = "income note",
                transactionDate = LocalDateTime.now(),
                category = null
            )
        )
    }

    private fun createExpenseTransaction(userId: UUID, category: Category, amount: BigDecimal): Transaction {
        return transactionRepository.save(
            Transaction(
                userId = userId,
                title = "Expense",
                amount = amount,
                note = "expense note",
                transactionDate = LocalDateTime.now(),
                category = category
            )
        )
    }
}
