package org.spendoo.identity.repository

import org.spendoo.identity.entity.RefreshToken
import org.spendoo.identity.entity.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface RefreshTokenRepository : JpaRepository<RefreshToken, Long> {

    fun findByToken(token: String): Optional<RefreshToken>
    fun findByUser(user: User): Optional<RefreshToken>
    fun deleteByUser(user: User)
}