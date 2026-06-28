package org.spendoo.savingGoals.scheduler

import org.spendoo.savingGoals.service.AchievementService
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class AchievementScheduler(
    private val achievementService: AchievementService
) {

    @Scheduled(cron = "0 0 0 * * SUN")
    fun syncMissingAchievements() {
        achievementService.syncMissingAchievementsForAllUsers()
    }
}
