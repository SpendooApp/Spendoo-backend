package org.spendoo.transactions.eventListener

import org.spendoo.transactions.repository.CategoryRepository
import org.spendoo.events.identity.UserCreatedEvent
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class UserCreatedListener(
    private val categoryRepository: CategoryRepository
) {
    @Async
    @EventListener
    @Transactional
    fun handleUserCreatedEvent(user: UserCreatedEvent) {
        categoryRepository.insertDefaultCategoriesForUser(user.id)
    }
}
