package org.spendoo.savingGoals.eventListener

import org.spendoo.events.identity.UserCreatedEvent
import org.spendoo.savingGoals.service.AchievementService
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component

class UserCreatedListener(
    private val achievementService: AchievementService
) {
    @Async
    @EventListener
    @Transactional
    fun handleUserCreatedEvent(user: UserCreatedEvent) {
        achievementService.createDefaultAchievementsForUser(user.id)
    }
}
