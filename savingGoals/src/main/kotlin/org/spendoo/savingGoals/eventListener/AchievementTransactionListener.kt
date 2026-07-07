package org.spendoo.savingGoals.eventListener

import org.spendoo.events.transactions.TransactionCreatedEvent
import org.spendoo.savingGoals.service.AchievementService
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.event.TransactionalEventListener

@Component
class AchievementTransactionListener(
    private val achievementService: AchievementService
) {
    @Async
    @TransactionalEventListener(fallbackExecution = true)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun handleTransactionCreated(event: TransactionCreatedEvent) {
        achievementService.handleTransactionCreated(event.userId, event.distinctCategoryCount)
    }
}