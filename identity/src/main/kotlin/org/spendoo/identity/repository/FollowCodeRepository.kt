package org.spendoo.identity.repository

import org.spendoo.identity.entity.FollowCode
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface FollowCodeRepository: JpaRepository<FollowCode, UUID> {

    fun findByCode(code: String): FollowCode?
    fun findByUserId(userId: UUID): FollowCode?
    fun existsByCode(code: String): Boolean
    fun deleteByUserId(userId: UUID)
}