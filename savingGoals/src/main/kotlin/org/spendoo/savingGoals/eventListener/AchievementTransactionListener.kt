package org.spendoo.savingGoals.eventListener

import org.spendoo.events.transactions.TransactionCreatedEvent
import org.spendoo.savingGoals.service.AchievementService
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class AchievementTransactionListener(
    private val achievementService: AchievementService
) {
    @Async
    @EventListener
    @Transactional
    fun handleTransactionCreated(event: TransactionCreatedEvent) {
        achievementService.handleTransactionCreated(event.userId, event.distinctCategoryCount)
    }
}