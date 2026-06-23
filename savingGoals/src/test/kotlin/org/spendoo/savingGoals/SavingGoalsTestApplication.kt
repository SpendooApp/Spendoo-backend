package org.spendoo.savingGoals

import org.spendoo.savingGoals.repository.SavingBalanceRepository
import org.spendoo.savingGoals.repository.SavingGoalHistoryRepository
import org.spendoo.savingGoals.repository.SavingGoalRepository
import org.spendoo.savingGoals.service.AchievementService
import org.spendoo.savingGoals.service.SavingGoalService
import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.persistence.autoconfigure.EntityScan
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.data.jpa.repository.config.EnableJpaRepositories

@SpringBootConfiguration
@EnableAutoConfiguration
@EntityScan(basePackages = ["org.spendoo.savingGoals.entity"])
@EnableJpaRepositories(basePackages = ["org.spendoo.savingGoals.repository"])
@ComponentScan(basePackages = ["org.spendoo"])
class SavingGoalsTestApplication {

    @Bean
    fun savingGoalService(
        savingGoalRepository: SavingGoalRepository,
        savingBalanceRepository: SavingBalanceRepository,
        savingGoalHistoryRepository: SavingGoalHistoryRepository,
        achievementService: AchievementService

    ): SavingGoalService {
        return SavingGoalService(
            savingGoalRepository = savingGoalRepository,
            savingBalanceRepository = savingBalanceRepository,
            savingGoalHistoryRepository = savingGoalHistoryRepository,
            achievementService = achievementService
        )
    }
}