package org.spendoo.identity.service

import org.spendoo.events.notifications.EmailEvent
import org.spendoo.events.publisher.SpendooEventPublisher
import org.springframework.stereotype.Service
import java.security.SecureRandom

@Service
class EmailService(
    private val publisher: SpendooEventPublisher,
) {
    fun sendOtp(email: String, otp: String) {
        val text = """
            Hello,
            
            you have requested to reset your password.
            Your OTP code is: $otp
            
            This code will expire in 15 minutes.
            If you did not request this, please ignore this email.
            
            Thanks,
            Spendoo Team
        """.trimIndent()

        val event = EmailEvent(
            to = email,
            subject = "Spendoo - Password Reset Code",
            text = text,
        )

        publisher.publish(event)
    }

    fun sendWelcomeVerificationOtp(email: String, otp: String) {
        val text = """
            Welcome to Spendoo!
            
            To complete your registration, please use the following OTP code: $otp
            
            This code will expire in 15 minutes.
            
            Thanks,
            Spendoo Team
        """.trimIndent()

        val event = EmailEvent(
            to = email,
            subject = "Spendoo - Welcome! Verify your email",
            text = text,
        )

        publisher.publish(event)
    }

    fun generateOtp(): String {
        val secureRandom = SecureRandom()
        val number = secureRandom.nextInt(10000)
        return String.format("%05d", number)
    }

}