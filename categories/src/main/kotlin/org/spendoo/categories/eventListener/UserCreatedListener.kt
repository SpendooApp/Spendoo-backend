package org.spendoo.categories.eventListener

import org.spendoo.categories.repository.CategoryRepository
import org.spendoo.events.identity.UserCreatedEvent
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component

@Component
class UserCreatedListener(
    private val categoryRepository: CategoryRepository
) {
    @Async
    @EventListener
    fun handleEmailEvent(user: UserCreatedEvent) {
        categoryRepository.insertDefaultCategoriesForUser(user.id)
    }
}