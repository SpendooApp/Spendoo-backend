package org.spendoo.transactions.repository

import org.spendoo.transactions.entity.UserAIUsage
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface UserAIUsageRepository: JpaRepository<UserAIUsage, UUID> {
    fun findByUserId(userId: UUID): UserAIUsage?
}