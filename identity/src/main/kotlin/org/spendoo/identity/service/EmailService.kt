package org.spendoo.identity.service

import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.stereotype.Service
import java.security.SecureRandom

@Service
class EmailService (
    private val mailSender: JavaMailSender,
){
    fun sendOtp (email: String, otp: String) {
        val message = SimpleMailMessage()
        message.from = "noreply@spendoo.com"
        message.setTo(email)
        message.subject = "Spendoo - Password Reset Code"
        message.text = """
            Hello,
            
            you have requested to reset your password.
            Your OTP code is: $otp
            
            This code will expire in 15 minutes.
            If you did not request this, please ignore this email.
            
            Thanks,
            Spendoo Team
        """.trimIndent()
        mailSender.send(message)
    }

    fun sendWelcomeVerificationOtp(email: String, otp: String) {
        val message = SimpleMailMessage()
        message.from = "noreply@spendoo.com"
        message.setTo(email)
        message.subject = "Spendoo - Welcome! Verify your email"
        message.text = """
            Welcome to Spendoo!
            
            To complete your registration, please use the following OTP code: $otp
            
            This code will expire in 15 minutes.
            
            Thanks,
            Spendoo Team
        """.trimIndent()
        mailSender.send(message)
    }

    fun generateOtp(): String {
        val secureRandom = SecureRandom()
        val number = secureRandom.nextInt(10000)
        return String.format("%05d", number)
    }


}