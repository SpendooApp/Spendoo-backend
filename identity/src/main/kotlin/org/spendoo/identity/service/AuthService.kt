package org.spendoo.identity.service

import jakarta.persistence.EntityNotFoundException
import jakarta.transaction.Transactional
import org.apache.coyote.BadRequestException
import org.spendoo.identity.api.dto.request.ForgotPasswordRequest
import org.spendoo.identity.api.dto.response.AuthResponse
import org.spendoo.identity.api.dto.request.LoginRequest
import org.spendoo.identity.api.dto.request.RefreshTokenRequest
import org.spendoo.identity.api.dto.request.RegisterRequest
import org.spendoo.identity.api.dto.request.ResetPasswordRequest
import org.spendoo.identity.api.dto.request.VerifyOtpRequest
import org.spendoo.identity.entity.EmailVerification
import org.spendoo.identity.entity.RefreshToken
import org.spendoo.identity.entity.User
import org.spendoo.identity.exception.TokenExpiredException
import org.spendoo.identity.exception.UnauthorizedException
import org.spendoo.identity.exception.UserAlreadyExistsException
import org.spendoo.identity.service.mapper.toEntity
import org.spendoo.identity.repository.EmailVerificationRepository
import org.spendoo.identity.repository.RefreshTokenRepository
import org.spendoo.identity.repository.UserRepository
import org.spendoo.identity.security.JwtUtil
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.UUID

@Service
@Transactional
class AuthService(
    private val userRepository: UserRepository,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val otpRepository: EmailVerificationRepository,
    private val emailService: EmailService,
    private val passwordEncoder: PasswordEncoder,
    private val jwtUtil: JwtUtil,
    private val authenticationManager: AuthenticationManager
) {

    fun register(request: RegisterRequest): String {
        val user = userRepository.findByEmail(request.email)

        val userToSave = user?.let {
            if (user.isVerified) throw UserAlreadyExistsException("Email is already registered and verified.")
            request.toEntity(passwordEncoder.encode(request.password)!!, id = user.id)
        } ?: run {
            request.toEntity(passwordEncoder.encode(request.password)!!)
        }

        val savedUser = userRepository.save(userToSave)

        val otpCode = emailService.generateOtp()
        val verificationToken = EmailVerification(
            otp = otpCode,
            isUsed = false,
            user = savedUser
        )
        otpRepository.save(verificationToken)
        emailService.sendWelcomeVerificationOtp(savedUser.email, otpCode)

        return "Registration successful. Please check your email for the verification code."
    }

    fun verifyAccount(request: VerifyOtpRequest): AuthResponse {
        val user = getUserByEmailOrThrow(request.email)

        val token = otpRepository.findByOtpAndUser(request.otp, user) ?: throw RuntimeException("Invalid OTP")

        if (token.isExpired() || token.isUsed) throw RuntimeException("Invalid or expired OTP")

        val verifiedUser = user.copy(isVerified = true)
        val savedUser = userRepository.save(verifiedUser)

        val usedToken = token.copy(isUsed = true, user = savedUser)
        otpRepository.save(usedToken)

        val accessToken = jwtUtil.generateAccessToken(verifiedUser.email)
        val refreshToken = jwtUtil.generateRefreshToken(verifiedUser.email)
        saveRefreshToken(verifiedUser, refreshToken)

        return AuthResponse(accessToken, refreshToken)
    }

    fun login(request: LoginRequest): AuthResponse {
        val user = getUserByEmailOrThrow(request.email)

        if (!user.isVerified) {
            throw UnauthorizedException("Please verify your email before logging in.")
        }
        authenticationManager.authenticate(
            UsernamePasswordAuthenticationToken(request.email, request.password)
        )
        val accessToken = jwtUtil.generateAccessToken(request.email)
        val refreshToken = jwtUtil.generateRefreshToken(request.email)

        saveRefreshToken(user, refreshToken)

        return AuthResponse(accessToken, refreshToken)
    }


    fun refreshToken(request: RefreshTokenRequest): AuthResponse {

        val refreshTokenEntity = refreshTokenRepository.findByToken(request.refreshToken)
            .orElseThrow { UnauthorizedException("Invalid refresh token") }

        val user = refreshTokenEntity.user

        refreshTokenRepository.delete(refreshTokenEntity)

        if (refreshTokenEntity.expiryDate.isBefore(LocalDateTime.now())) {
            throw TokenExpiredException("Refresh token is expired. Please login again.")
        }

        if (jwtUtil.validateRefreshToken(request.refreshToken) &&
            jwtUtil.validateTokenForUser(request.refreshToken, user.email)) {

            val newAccessToken = jwtUtil.generateAccessToken(user.email)
            val newRefreshToken = jwtUtil.generateRefreshToken(user.email)


            saveRefreshToken(user, newRefreshToken)

            return AuthResponse(newAccessToken, newRefreshToken)
        } else {
            throw UnauthorizedException("Invalid refresh token")
        }
    }

    private fun saveRefreshToken(user: User, token: String) {
        val expiryDate = LocalDateTime.now().plusDays(14)

        refreshTokenRepository.save(
            RefreshToken(token = token, expiryDate = expiryDate, user = user)
        )
    }

    fun logout(userId: UUID, request: RefreshTokenRequest) {
        refreshTokenRepository.findByUserIdAndToken(userId = userId, request.refreshToken).ifPresent { tokenEntity ->
            refreshTokenRepository.delete(tokenEntity)
        }
    }

    fun forgotPassword(request: ForgotPasswordRequest): String {
        val user = getUserByEmailOrThrow(request.email)

        if (!user.isVerified) {
            throw RuntimeException("Account is not verified yet. Please verify your email first.")
        }

        val otpCode = emailService.generateOtp()

        val resetToken = EmailVerification(
            otp = otpCode,
            isUsed = false,
            user = user
        )
        otpRepository.save(resetToken)
        emailService.sendOtp(user.email, otpCode)
        return "OTP sent successfully to your email."
    }

    fun verifyOtp(request: VerifyOtpRequest): String {
        val user = getUserByEmailOrThrow(request.email)

        val token = otpRepository.findByOtpAndUser(request.otp, user)
            ?: throw RuntimeException("Invalid OTP")

        if (token.isExpired()) throw RuntimeException("OTP has expired")
        if (token.isUsed) throw RuntimeException("OTP has already been used")

        return "OTP verified successfully. You can now reset your password."
    }

    fun resetPassword(request: ResetPasswordRequest): String {
        val user = getUserByEmailOrThrow(request.email)

        val token = otpRepository.findByOtpAndUser(request.otp, user)
            ?: throw RuntimeException("Invalid OTP")

        if (token.isExpired() || token.isUsed) throw BadRequestException("Invalid or expired OTP")
        val updatedUser = user.copy(
            passwordHash = passwordEncoder.encode(request.newPassword)!!
        )
        val savedUser = userRepository.save(updatedUser)

        val usedToken = token.copy(
            isUsed = true,
            user = savedUser
        )
        otpRepository.save(usedToken)

        return "Password reset successfully. You can now login."
    }

    fun resendOtp(request: ForgotPasswordRequest): String {
        val user = getUserByEmailOrThrow(request.email)


        val otpCode = emailService.generateOtp()
        val verificationToken = EmailVerification(
            otp = otpCode,
            isUsed = false,
            user = user
        )
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
        return userRepository.findByEmail(email) ?: throw EntityNotFoundException("User not found with this email")
    }
}