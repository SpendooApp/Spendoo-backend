package org.spendoo.identity.repository

import org.spendoo.identity.entity.UserSubscription
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface UserSubscriptionRepository: JpaRepository<UserSubscription, UUID> {
    fun findByUserId(userId: UUID): UserSubscription?
}