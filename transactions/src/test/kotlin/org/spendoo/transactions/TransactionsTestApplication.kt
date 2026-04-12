package org.spendoo.transactions

import org.spendoo.transactions.repository.BudgetRepository
import org.spendoo.transactions.repository.CategoryRepository
import org.spendoo.transactions.repository.TransactionRepository
import org.spendoo.transactions.service.BudgetService
import org.spendoo.transactions.service.CategoryService
import org.spendoo.transactions.service.TransactionService
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
    fun budgetService(
        budgetRepository: BudgetRepository,
        transactionRepository: TransactionRepository
    ): BudgetService {
        return BudgetService(
            budgetRepository = budgetRepository,
            transactionRepository = transactionRepository
        )
    }

    @Bean
    fun categoryService(
        categoryRepository: CategoryRepository,
        budgetService: BudgetService
    ): CategoryService {
        return CategoryService(
            categoryRepository = categoryRepository,
            budgetService = budgetService
        )
    }

    @Bean
    fun transactionService(
        transactionRepository: TransactionRepository,
        categoryRepository: CategoryRepository
    ): TransactionService {
        return TransactionService(
            transactionRepository = transactionRepository,
            categoryRepository = categoryRepository
        )
    }
}


