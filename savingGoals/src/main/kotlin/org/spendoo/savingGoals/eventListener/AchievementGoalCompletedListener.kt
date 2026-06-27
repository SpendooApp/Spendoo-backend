package org.spendoo.savingGoals.eventListener

import org.spendoo.events.savings.SavingGoalCompletedEvent
import org.spendoo.savingGoals.service.AchievementService
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class AchievementGoalCompletedListener(
    private val achievementService: AchievementService
) {
    @Async
    @EventListener
    @Transactional
    fun handleSavingGoalCompleted(event: SavingGoalCompletedEvent) {
        achievementService.checkGoalAchievements(event.userId)
    }
}
