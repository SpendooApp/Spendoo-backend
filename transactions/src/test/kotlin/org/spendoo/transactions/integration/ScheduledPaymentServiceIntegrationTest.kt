package org.spendoo.transactions.integration
import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.spendoo.transactions.TransactionsTestApplication
import org.spendoo.transactions.api.dto.request.PaymentRequest
import org.spendoo.transactions.api.dto.request.alignNextDueDate
import org.spendoo.transactions.entity.Category
import org.spendoo.transactions.entity.CategoryIcon
import org.spendoo.transactions.entity.LeftOverOptions
import org.spendoo.transactions.entity.ReminderUnit
import org.spendoo.transactions.entity.ScheduledPayment
import org.spendoo.transactions.repository.CategoryRepository
import org.spendoo.transactions.repository.ScheduledPaymentRepository
import org.spendoo.transactions.repository.TransactionRepository
import org.spendoo.transactions.service.ScheduledPaymentService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.domain.PageRequest
import org.springframework.test.context.ActiveProfiles
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.*

@SpringBootTest(classes = [TransactionsTestApplication::class])
@ActiveProfiles("test")
class ScheduledPaymentServiceIntegrationTest {

    @Autowired
    private lateinit var scheduledPaymentService: ScheduledPaymentService

    @Autowired
    private lateinit var paymentRepository: ScheduledPaymentRepository

    @Autowired
    private lateinit var transactionRepository: TransactionRepository

    @Autowired
    private lateinit var categoryRepository: CategoryRepository

    private lateinit var existingUserId: UUID
    private lateinit var existingCategory: Category

    @BeforeEach
    fun setUp() {
        paymentRepository.deleteAll()
        transactionRepository.deleteAll()
        categoryRepository.deleteAll()

        existingUserId = UUID.randomUUID()
        existingCategory = createCategory(existingUserId, "Utilities")
    }


    @Test
    fun `createPayment saves new scheduled payment successfully`() {
        val request = PaymentRequest(
            title = "Electricity Bill",
            amount = BigDecimal.valueOf(250.0),
            categoryId = existingCategory.id,
            frequency = 30,
            startDate = LocalDateTime.now(),
            reminderPeriod = 1,
            reminderUnit = ReminderUnit.DAY
        )

        scheduledPaymentService.createPayment(existingUserId, request)

        val savedPayments = paymentRepository.findAllByUserId(existingUserId, PageRequest.of(0, 10))
        assertThat(savedPayments.totalElements).isEqualTo(1)
        assertThat(savedPayments.content.first().title).isEqualTo("Electricity Bill")
        assertThat(savedPayments.content.first().amount.compareTo(BigDecimal.valueOf(250.0))).isEqualTo(0)
    }

    @Test
    fun `updatePayment updates fields and shifts dates if startDate changed`() {
        val oldStartDate = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS).minusDays(5)
        val existingPayment = createScheduledPayment(
            title = "Old Sub",
            amount = BigDecimal.valueOf(100.0),
            startDate = oldStartDate,
            frequency = 7,
            isNotified = true
        )

        val newStartDate = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS)
        val request = PaymentRequest(
            title = "New Sub",
            amount = BigDecimal.valueOf(150.0),
            categoryId = existingCategory.id,
            frequency = 30,
            startDate = newStartDate,
            reminderPeriod = 2,
            reminderUnit = ReminderUnit.DAY
        )

        scheduledPaymentService.updatePayment(existingUserId, existingPayment.id, request)

        val updatedPayment = paymentRepository.findById(existingPayment.id).orElseThrow()
        assertThat(updatedPayment.title).isEqualTo("New Sub")
        assertThat(updatedPayment.amount.compareTo(BigDecimal.valueOf(150.0))).isEqualTo(0)

        val expectedNextDueDate = request.frequency.alignNextDueDate(request.startDate)
        val expectedReminderDate = expectedNextDueDate.minusDays(2)

        assertThat(updatedPayment.nextDueDate).isEqualTo(expectedNextDueDate)
        assertThat(updatedPayment.nextReminderDate).isEqualTo(expectedReminderDate)

        assertThat(updatedPayment.isNotified).isFalse()
    }

    @Test
    fun `payScheduledItem creates expense transaction and shifts payment cycle`() {
        val oldNextDueDate = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS).plusDays(2)
        val existingPayment = createScheduledPayment(
            title = "Gym Membership",
            amount = BigDecimal.valueOf(500.0),
            startDate = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS).minusMonths(1),
            nextDueDate = oldNextDueDate,
            frequency = 30,
            isNotified = true
        )

        scheduledPaymentService.payScheduledItem(existingUserId, existingPayment.id)

        val updatedPayment = paymentRepository.findById(existingPayment.id).orElseThrow()

        assertThat(updatedPayment.startDate).isEqualTo(oldNextDueDate)

        val expectedNewDueDate = oldNextDueDate.plusDays(30)
        val expectedNewReminderDate = expectedNewDueDate.minusDays(1)

        assertThat(updatedPayment.nextDueDate).isEqualTo(expectedNewDueDate)
        assertThat(updatedPayment.nextReminderDate).isEqualTo(expectedNewReminderDate)
        assertThat(updatedPayment.isNotified).isFalse()

        val savedTransactions = transactionRepository.findAllByUserId(existingUserId, PageRequest.of(0, 10))
        assertThat(savedTransactions.totalElements).isEqualTo(1)
        val expenseTx = savedTransactions.content.first()
        assertThat(expenseTx.title).isEqualTo("Gym Membership")
        assertThat(expenseTx.amount.compareTo(BigDecimal.valueOf(-500.0))).isEqualTo(0)
        assertThat(expenseTx.category?.id).isEqualTo(existingCategory.id)
    }

    @Test
    fun `skipPayment shifts payment cycle without creating expense transaction`() {
        val oldNextDueDate = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS).plusDays(5)
        val existingPayment = createScheduledPayment(
            title = "Netflix",
            amount = BigDecimal.valueOf(150.0),
            startDate = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS).minusMonths(1),
            nextDueDate = oldNextDueDate,
            frequency = 30,
            isNotified = true
        )

        scheduledPaymentService.skipPayment(existingUserId, existingPayment.id)

        val updatedPayment = paymentRepository.findById(existingPayment.id).orElseThrow()
        assertThat(updatedPayment.startDate).isEqualTo(oldNextDueDate)

        val expectedNewDueDate = oldNextDueDate.plusDays(30)
        val expectedNewReminderDate = expectedNewDueDate.minusDays(1)

        assertThat(updatedPayment.nextDueDate).isEqualTo(expectedNewDueDate)
        assertThat(updatedPayment.nextReminderDate).isEqualTo(expectedNewReminderDate)
        assertThat(updatedPayment.isNotified).isFalse()

        val savedTransactions = transactionRepository.findAllByUserId(existingUserId, PageRequest.of(0, 10))
        assertThat(savedTransactions.totalElements).isEqualTo(0)
    }

    @Test
    fun `deletePayment removes the scheduled payment completely`() {
        val existingPayment = createScheduledPayment(title = "To Delete")

        scheduledPaymentService.deletePayment(existingUserId, existingPayment.id)

        val paymentExists = paymentRepository.existsById(existingPayment.id)
        assertThat(paymentExists).isFalse()
    }

    @Test
    fun `getAllPayments returns dashboard summary with correct totals`() {
        createScheduledPayment(title = "Bill 1", amount = BigDecimal.valueOf(200.0))
        createScheduledPayment(title = "Bill 2", amount = BigDecimal.valueOf(300.0))

        val dashboardResponse = scheduledPaymentService.getDashboardSummary(existingUserId)

        assertThat(dashboardResponse.upcomingCount).isEqualTo(2)
        assertThat(dashboardResponse.totalScheduledAmount.compareTo(BigDecimal.valueOf(500.0))).isEqualTo(0)
    }

    @Test
    fun `getPaymentEntity throws exception if unauthorized user attempts access`() {
        val existingPayment = createScheduledPayment(title = "My Secret Bill")
        val hackerUserId = UUID.randomUUID()

        val thrownException = assertThrows<RuntimeException> {
            scheduledPaymentService.updatePayment(
                hackerUserId,
                existingPayment.id,
                PaymentRequest("Hacked", BigDecimal.ZERO, existingCategory.id, LocalDateTime.now(), 7, 1, ReminderUnit.DAY)
            )
        }

        assertThat(thrownException).hasMessageThat().contains("Unauthorized")
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

    private fun createScheduledPayment(
        title: String,
        amount: BigDecimal = BigDecimal.valueOf(100.0),
        startDate: LocalDateTime = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS),
        nextDueDate: LocalDateTime = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS).plusDays(30),
        nextReminderDate: LocalDateTime = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS).plusDays(29),
        frequency: Int = 30,
        isNotified: Boolean = false
    ): ScheduledPayment {
        return paymentRepository.save(
            ScheduledPayment(
                userId = existingUserId,
                category = existingCategory,
                title = title,
                amount = amount,
                startDate = startDate,
                nextDueDate = nextDueDate,
                nextReminderDate = nextReminderDate,
                frequency = frequency,
                reminderPeriod = 1,
                reminderUnit = ReminderUnit.DAY,
                isNotified = isNotified
            )
        )
    }
}