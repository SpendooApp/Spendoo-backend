package org.spendoo.transactions.eventListener

import org.spendoo.events.identity.UserCreatedEvent
import org.spendoo.transactions.service.CategoryService
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class UserCreatedListener(
    private val categoryService: CategoryService
) {
    @Async
    @EventListener
    @Transactional
    fun handleUserCreatedEvent(user: UserCreatedEvent) {
        categoryService.createDefaultCategoriesForUser(user.id)
    }
}
