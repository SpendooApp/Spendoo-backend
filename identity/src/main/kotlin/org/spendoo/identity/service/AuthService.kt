package org.spendoo.identity.service

import jakarta.persistence.EntityNotFoundException
import jakarta.transaction.Transactional
import org.spendoo.events.publisher.SpendooEventPublisher
import org.spendoo.identity.api.dto.request.*
import org.spendoo.identity.api.dto.response.AuthResponse
import org.spendoo.identity.entity.EmailVerification
import org.spendoo.identity.entity.RefreshToken
import org.spendoo.identity.entity.User
import org.spendoo.identity.exception.InvalidCredentialsException
import org.spendoo.identity.exception.TokenExpiredException
import org.spendoo.identity.exception.UnauthorizedException
import org.spendoo.identity.exception.UserAlreadyExistsException
import org.spendoo.identity.repository.EmailVerificationRepository
import org.spendoo.identity.repository.RefreshTokenRepository
import org.spendoo.identity.repository.UserRepository
import org.spendoo.identity.security.JwtUtil
import org.spendoo.identity.service.mapper.toEntity
import org.spendoo.identity.service.mapper.toUserCreatedEvent
import org.spendoo.events.identity.UserLoggedInEvent
import org.spendoo.identity.entity.PlanCode
import org.spendoo.identity.entity.UserSubscription
import org.spendoo.identity.repository.SubscriptionPlanRepository
import org.spendoo.identity.repository.UserSubscriptionRepository
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.*

@Service
@Transactional
class AuthService(
    private val userRepository: UserRepository,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val otpRepository: EmailVerificationRepository,
    private val subscriptionPlanRepository: SubscriptionPlanRepository,
    private val userSubscriptionRepository: UserSubscriptionRepository,
    private val emailService: EmailService,
    private val passwordEncoder: PasswordEncoder,
    private val jwtUtil: JwtUtil,
    private val spendooEventPublisher: SpendooEventPublisher
) {

    fun register(request: RegisterRequest): String {
        val lowerCaseEmail = request.email.lowercase()
        val user = userRepository.findByEmail(lowerCaseEmail)

        val userToSave = user?.let {
            if (user.isVerified) throw UserAlreadyExistsException("Email is already registered and verified.")
            request.toEntity(passwordEncoder.encode(request.password)!!, id = user.id)
        } ?: run {
            request.toEntity(passwordEncoder.encode(request.password)!!)
        }

        val finalUserToSave = userToSave.copy(email = lowerCaseEmail)
        val savedUser = userRepository.save(finalUserToSave)

        val freePlan = subscriptionPlanRepository.findByCode(PlanCode.FREE)
            ?: throw EntityNotFoundException("FREE subscription plan not found")

        userSubscriptionRepository.save(
            UserSubscription(
                userId = savedUser.id,
                subscriptionPlan = freePlan,
                billingCycle = null
            )
        )

        val otpCode = emailService.generateOtp()
        val verificationToken = EmailVerification(otp = otpCode, user = savedUser)
        otpRepository.save(verificationToken)
        emailService.sendWelcomeVerificationOtp(savedUser.email, otpCode)

        return "Registration successful. Please check your email for the verification code."
    }

    fun verifyAccount(request: VerifyOtpRequest): AuthResponse {
        val user = getUserByEmailOrThrow(request.email)

        val token = otpRepository.findByOtpAndUser(request.otp, user) ?: throw RuntimeException("Invalid or expired OTP")

        if (token.isExpired()) otpRepository.delete(token).also {
            throw RuntimeException("OTP has expired")
        }

        val verifiedUser = user.copy(isVerified = true)
        userRepository.save(verifiedUser)
        spendooEventPublisher.publish(verifiedUser.toUserCreatedEvent())
        otpRepository.delete(token)

        val accessToken = jwtUtil.generateAccessToken(verifiedUser.id)
        val refreshToken = jwtUtil.generateRefreshToken(verifiedUser.id)
        saveRefreshToken(verifiedUser, refreshToken, request.deviceToken)

        return AuthResponse(accessToken, refreshToken)
    }

    fun login(request: LoginRequest): AuthResponse {
        val user = getUserByEmailOrThrow(request.email)

        if (!passwordEncoder.matches(request.password, user.passwordHash)) {
            throw InvalidCredentialsException()
        }

        if (!user.isVerified) {
            throw UnauthorizedException("Please verify your email before logging in.")
        }

        val accessToken = jwtUtil.generateAccessToken(user.id)
        val refreshToken = jwtUtil.generateRefreshToken(user.id)

        saveRefreshToken(user, refreshToken, request.deviceToken)
        spendooEventPublisher.publish(UserLoggedInEvent(user.id))

        return AuthResponse(accessToken, refreshToken)
    }


    fun refreshToken(request: RefreshTokenRequest): AuthResponse {

        val refreshTokenEntity = refreshTokenRepository.findByToken(request.refreshToken)
            ?: throw UnauthorizedException("Invalid refresh token")

        val user = refreshTokenEntity.user
        val oldDeviceToken = refreshTokenEntity.deviceToken

        refreshTokenRepository.delete(refreshTokenEntity)

        if (refreshTokenEntity.expiryDate.isBefore(LocalDateTime.now())) {
            throw TokenExpiredException("Refresh token is expired. Please login again.")
        }

        if (jwtUtil.validateRefreshToken(request.refreshToken) &&
            jwtUtil.validateTokenForUser(request.refreshToken, user.id)) {

            val newAccessToken = jwtUtil.generateAccessToken(user.id)
            val newRefreshToken = jwtUtil.generateRefreshToken(user.id)

            val finalDeviceToken = request.deviceToken ?: oldDeviceToken
            saveRefreshToken(user, newRefreshToken, finalDeviceToken)
            spendooEventPublisher.publish(UserLoggedInEvent(user.id))

            return AuthResponse(newAccessToken, newRefreshToken)
        } else {
            throw UnauthorizedException("Invalid refresh token")
        }
    }

    private fun saveRefreshToken(user: User, token: String, deviceToken: String? = null) {
        val expiryDate = LocalDateTime.now().plusDays(14)

        refreshTokenRepository.save(
            RefreshToken(token = token, expiryDate = expiryDate, user = user, deviceToken = deviceToken)
        )
    }

    fun updateDeviceToken(userId: UUID, refreshToken: String, deviceToken: String) {
        val tokenEntity = refreshTokenRepository.findByUserIdAndToken(userId, refreshToken)
            ?: throw UnauthorizedException("Invalid refresh token")
        
        val updatedEntity = tokenEntity.copy(deviceToken = deviceToken)
        refreshTokenRepository.save(updatedEntity)
    }

    fun logout(userId: UUID, request: RefreshTokenRequest) {
        refreshTokenRepository.findByUserIdAndToken(userId = userId, request.refreshToken)?.let { tokenEntity ->
            refreshTokenRepository.delete(tokenEntity)
        }
    }

    fun forgotPassword(request: ForgotPasswordRequest): String {
        val user = getUserByEmailOrThrow(request.email)

        if (!user.isVerified) {
            throw RuntimeException("Account is not verified yet. Please verify your email first.")
        }

        val otpCode = emailService.generateOtp()

        val resetToken = EmailVerification(otp = otpCode, user = user)

        otpRepository.save(resetToken)
        emailService.sendOtp(user.email, otpCode)
        return "OTP sent successfully to your email."
    }

    fun verifyOtp(request: VerifyOtpRequest): String {
        val user = getUserByEmailOrThrow(request.email)

        val token = otpRepository.findByOtpAndUser(request.otp, user)
            ?: throw RuntimeException("Invalid or expired OTP")

        if (token.isExpired()) otpRepository.delete(token).also {
            throw RuntimeException("OTP has expired")
        }

        return "OTP verified successfully. You can now reset your password."
    }

    fun resetPassword(request: ResetPasswordRequest): String {
        val user = getUserByEmailOrThrow(request.email)

        val token = otpRepository.findByOtpAndUser(request.otp, user)
            ?: throw RuntimeException("Invalid OTP")

        if (token.isExpired()) otpRepository.delete(token).also {
            throw RuntimeException("Invalid or expired OTP")
        }

        val updatedUser = user.copy(
            passwordHash = passwordEncoder.encode(request.newPassword)!!
        )
        userRepository.save(updatedUser)

        return "Password reset successfully. You can now login."
    }

    fun resendOtp(request: ForgotPasswordRequest): String {
        val user = getUserByEmailOrThrow(request.email)

        val otpCode = emailService.generateOtp()
        val verificationToken = EmailVerification(otp = otpCode, user = user)
        otpRepository.save(verificationToken)

        if (!user.isVerified) {
            emailService.sendWelcomeVerificationOtp(user.email, otpCode)
            return "Verification OTP resent successfully to your email."
        } else {
            emailService.sendOtp(user.email, otpCode)
            return "Password reset OTP resent successfully to your email."
        }
    }

    private fun getUserByEmailOrThrow(email: String): User {
        val lowerCaseEmail = email.lowercase()
        return userRepository.findByEmail(lowerCaseEmail) ?: throw EntityNotFoundException("User not found with this email")
    }

    @Scheduled(cron = "0 0 0 * * *")
    fun clearExpiredRefreshTokens() {
        val now = LocalDateTime.now()
        refreshTokenRepository.deleteAllByExpiryDateBefore(now)
    }

    @Scheduled(cron = "0 0 0 * * *")
    fun clearExpiredOtps() {
        val now = LocalDateTime.now()
        otpRepository.deleteAllBySentAtBefore(now.minusMinutes(15))
    }

    @Scheduled(cron = "0 0 0 * * *")
    fun clearUnverifiedUsers() {
        val cutoffDate = LocalDateTime.now().minusDays(1)
        userRepository.deleteAllByIsVerifiedIsFalseAndCreatedAtBefore(cutoffDate)
    }
}