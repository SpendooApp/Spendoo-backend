package org.spendoo.identity.repository

import org.spendoo.identity.entity.EmailVerification
import org.spendoo.identity.entity.User
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface EmailVerificationRepository : JpaRepository<EmailVerification, UUID> {
    fun findByOtpAndUser(otp: String, user: User): EmailVerification?
}