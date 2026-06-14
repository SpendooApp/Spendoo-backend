package org.spendoo.statistics

import org.spendoo.statistics.service.StatisticsService
import org.spendoo.transactions.repository.BudgetRepository
import org.spendoo.transactions.repository.TransactionRepository
import org.spendoo.transactions.repository.TransactionViewRepository
import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.persistence.autoconfigure.EntityScan
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.spendoo.i18n.I18nConfig
import org.spendoo.i18n.I18nService
import org.spendoo.storage.service.ImageStorageService
import io.mockk.mockk

@SpringBootConfiguration
@EnableAutoConfiguration
@EntityScan(basePackages = ["org.spendoo.transactions.entity"])
@EnableJpaRepositories(basePackages = ["org.spendoo.transactions.repository"])
@Import(I18nConfig::class, I18nService::class)
class StatisticsTestApplication {

    @Bean
    fun imageStorageService(): ImageStorageService {
        return mockk(relaxed = true)
    }

    @Bean
    fun statisticsService(
        transactionRepository: TransactionRepository,
        budgetRepository: BudgetRepository,
        transactionViewRepository: TransactionViewRepository,
        i18nService: I18nService,
        imageStorageService: ImageStorageService
    ): StatisticsService {
        return StatisticsService(
            transactionRepository = transactionRepository,
            budgetRepository = budgetRepository,
            transactionViewRepository = transactionViewRepository,
            i18nService = i18nService,
            imageStorageService = imageStorageService
        )
    }
}
