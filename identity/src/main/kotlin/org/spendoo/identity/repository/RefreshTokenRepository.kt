package org.spendoo.identity.repository

import org.spendoo.identity.entity.RefreshToken
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface RefreshTokenRepository : JpaRepository<RefreshToken, Long> {
    fun findByToken(token: String): RefreshToken?
    fun findByUserIdAndToken(userId: UUID, token: String): RefreshToken?
}