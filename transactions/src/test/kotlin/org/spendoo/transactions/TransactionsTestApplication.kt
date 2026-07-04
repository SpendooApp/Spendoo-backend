package org.spendoo.transactions

import io.mockk.mockk
import org.spendoo.client.ApiClient
import org.spendoo.transactions.repository.BudgetRepository
import org.spendoo.transactions.repository.CategoryRepository
import org.spendoo.transactions.repository.ScheduledPaymentRepository
import org.spendoo.transactions.repository.TransactionRepository
import org.spendoo.transactions.repository.TransactionViewRepository
import org.spendoo.transactions.service.BudgetService
import org.spendoo.transactions.service.CategoryService
import org.spendoo.transactions.service.ScheduledPaymentService
import org.spendoo.transactions.service.TransactionService
import org.spendoo.events.publisher.SpendooEventPublisher
import org.spendoo.transactions.repository.ProposedActionRepository
import org.spendoo.transactions.service.SmartBudgetService
import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.persistence.autoconfigure.EntityScan
import org.springframework.context.annotation.Bean
import org.springframework.data.jpa.repository.config.EnableJpaRepositories

@SpringBootConfiguration
@EnableAutoConfiguration
@EntityScan(basePackages = ["org.spendoo.transactions.entity"])
@EnableJpaRepositories(basePackages = ["org.spendoo.transactions.repository"])
class TransactionsTestApplication {

    @Bean
    fun spendooEventPublisher(): SpendooEventPublisher {
        return mockk<SpendooEventPublisher>(relaxed = true)
    }

    @Bean
    fun apiClient(): ApiClient {
        return mockk<ApiClient>(relaxed = true)
    }
    @Bean
    fun budgetService(
        budgetRepository: BudgetRepository,
        transactionRepository: TransactionRepository
    ): BudgetService {
        return BudgetService(
            budgetRepository = budgetRepository,
            transactionRepository = transactionRepository,
            apiClient = apiClient()
        )
    }

    @Bean
    fun categoryService(
        categoryRepository: CategoryRepository,
        budgetService: BudgetService,
        transactionRepository: TransactionRepository
    ): CategoryService {
        return CategoryService(
            categoryRepository = categoryRepository,
            budgetService = budgetService,
            transactionRepository = transactionRepository
        )
    }

    @Bean
    fun transactionService(
        transactionRepository: TransactionRepository,
        categoryRepository: CategoryRepository,
        transactionViewRepository: TransactionViewRepository,
        apiClient: ApiClient,
        spendooEventPublisher: SpendooEventPublisher,
        smartBudgetService: SmartBudgetService
    ): TransactionService {
        return TransactionService(
            transactionRepository = transactionRepository,
            categoryRepository = categoryRepository,
            transactionViewRepository = transactionViewRepository,
            apiClient = apiClient,
            spendooEventPublisher = spendooEventPublisher,
            smartBudgetService = smartBudgetService
        )
    }

    @Bean
    fun smartBudgetService(
        proposedActionRepository: ProposedActionRepository,
        categoryRepository: CategoryRepository,
        budgetRepository: BudgetRepository,
        apiClient: ApiClient,
        publisher: SpendooEventPublisher
    ): SmartBudgetService {
        return SmartBudgetService(
            proposedActionRepository = proposedActionRepository,
            budgetRepository = budgetRepository,
            categoryRepository = categoryRepository,
            apiClient = apiClient,
            publisher = publisher,
        )
    }

    @Bean
    fun scheduledPaymentService(
        paymentRepository: ScheduledPaymentRepository,
        transactionService: TransactionService,
        categoryRepository: CategoryRepository
    ): ScheduledPaymentService {
        return ScheduledPaymentService(
            paymentRepository = paymentRepository,
            transactionService = transactionService,
            categoryRepository = categoryRepository,
        )
    }

}


