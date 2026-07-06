package org.spendoo.identity.repository

import org.spendoo.identity.entity.User
import org.springframework.data.jpa.repository.JpaRepository
import java.time.Instant
import java.util.*

interface UserRepository : JpaRepository<User, UUID> {
    fun findByEmail(email: String): User?
    fun deleteAllByIsVerifiedIsFalseAndCreatedAtBefore(date: Instant)
}