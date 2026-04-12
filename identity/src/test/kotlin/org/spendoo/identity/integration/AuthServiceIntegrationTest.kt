package org.spendoo.identity.integration

import com.google.common.truth.Truth.assertThat
import io.mockk.every
import jakarta.persistence.EntityNotFoundException
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.spendoo.events.publisher.SpendooEventPublisher
import org.spendoo.identity.IdentityTestApplication
import org.spendoo.identity.api.dto.request.*
import org.spendoo.identity.entity.EmailVerification
import org.spendoo.identity.entity.Gender
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
import org.spendoo.identity.service.AuthService
import org.spendoo.identity.service.EmailService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.context.ActiveProfiles
import java.time.LocalDate
import java.time.LocalDateTime

@SpringBootTest(classes = [IdentityTestApplication::class])
@ActiveProfiles("test")
class AuthServiceIntegrationTest {

    @Autowired
    private lateinit var authService: AuthService

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var refreshTokenRepository: RefreshTokenRepository

    @Autowired
    private lateinit var emailVerificationRepository: EmailVerificationRepository

    @Autowired
    private lateinit var passwordEncoder: PasswordEncoder

    @Autowired
    private lateinit var emailService: EmailService

    @Autowired
    private lateinit var jwtUtil: JwtUtil

    @Autowired
    private lateinit var spendooEventPublisher: SpendooEventPublisher

    @BeforeEach
    fun setUp() {
        refreshTokenRepository.deleteAll()
        emailVerificationRepository.deleteAll()
        userRepository.deleteAll()
        every { emailService.generateOtp() } returns "12345"
        every { jwtUtil.generateAccessToken(any()) } returns "access-token"
        every { jwtUtil.generateRefreshToken(any()) } returns "refresh-token"
    }

    @Test
    fun `register returns success message if user does not exist`() {
        val request =
            RegisterRequest("New User", "new-user@mail.com", "Password@1", Gender.MALE, LocalDate.of(1998, 1, 1))

        val registrationMessage = authService.register(request)

        val savedUser = userRepository.findByEmail(request.email)
        val savedOtp = savedUser?.let { emailVerificationRepository.findByOtpAndUser("12345", it) }
        assertThat(registrationMessage).isEqualTo("Registration successful. Please check your email for the verification code.")
        assertThat(savedUser).isNotNull()
        assertThat(savedOtp).isNotNull()
    }

    @Test
    fun `register throw UserAlreadyExistsException if existing user is verified`() {
        val existingUser = createUser(email = "verified-user@mail.com", isVerified = true)
        val request =
            RegisterRequest("Verified User", existingUser.email, "Password@1", Gender.FEMALE, LocalDate.of(1995, 6, 10))

        val thrownException = assertThrows<UserAlreadyExistsException> { authService.register(request) }

        assertThat(thrownException).hasMessageThat().contains("Email is already registered and verified.")
    }

    @Test
    fun `register returns success message if existing user is unverified`() {
        val existingUser = createUser(email = "pending-user@mail.com", isVerified = false)
        val request = RegisterRequest(
            "Pending Updated",
            existingUser.email,
            "Password@1",
            Gender.FEMALE,
            LocalDate.of(1994, 3, 8)
        )

        val registrationMessage = authService.register(request)

        val updatedUser = userRepository.findByEmail(existingUser.email)
        assertThat(registrationMessage).isEqualTo("Registration successful. Please check your email for the verification code.")
        assertThat(updatedUser).isNotNull()
        assertThat(updatedUser?.id).isEqualTo(existingUser.id)
        assertThat(updatedUser?.fullName).isEqualTo("Pending Updated")
    }

    @Test
    fun `verifyAccount throw RuntimeException if otp is invalid`() {
        createUser(email = "otp-invalid@mail.com", isVerified = false)
        val request = VerifyOtpRequest(email = "otp-invalid@mail.com", otp = "99999")

        val thrownException = assertThrows<RuntimeException> { authService.verifyAccount(request) }

        assertThat(thrownException).hasMessageThat().contains("Invalid or expired OTP")
    }

    @Test
    fun `verifyAccount throw RuntimeException if otp is expired`() {
        val user = createUser(email = "otp-expired@mail.com", isVerified = false)
        emailVerificationRepository.save(
            EmailVerification(
                otp = "12345",
                sentAt = LocalDateTime.now().minusMinutes(20),
                user = user
            )
        )
        val request = VerifyOtpRequest(email = user.email, otp = "12345")

        val thrownException = assertThrows<RuntimeException> { authService.verifyAccount(request) }

        assertThat(thrownException).hasMessageThat().contains("OTP has expired")
    }

    @Test
    fun `verifyAccount returns auth response if otp is valid`() {
        val user = createUser(email = "otp-valid@mail.com", isVerified = false)
        emailVerificationRepository.save(
            EmailVerification(
                otp = "12345",
                sentAt = LocalDateTime.now().minusMinutes(1),
                user = user
            )
        )
        val request = VerifyOtpRequest(email = user.email, otp = "12345")

        val authResponse = authService.verifyAccount(request)

        val verifiedUser = userRepository.findByEmail(user.email)
        val savedRefreshToken = refreshTokenRepository.findByToken("refresh-token")
        assertThat(authResponse.accessToken).isEqualTo("access-token")
        assertThat(authResponse.refreshToken).isEqualTo("refresh-token")
        assertThat(verifiedUser?.isVerified).isTrue()
        assertThat(savedRefreshToken).isNotNull()
    }

    @Test
    fun `login throw InvalidCredentialsException if password is invalid`() {
        createUser(email = "invalid-password@mail.com", isVerified = true, plainPassword = "Password@1")
        val request = LoginRequest(email = "invalid-password@mail.com", password = "Password@2")

        val thrownException = assertThrows<InvalidCredentialsException> { authService.login(request) }

        assertThat(thrownException).hasMessageThat().contains("Invalid email or password")
    }

    @Test
    fun `login throw UnauthorizedException if user is not verified`() {
        createUser(email = "not-verified@mail.com", isVerified = false, plainPassword = "Password@1")
        val request = LoginRequest(email = "not-verified@mail.com", password = "Password@1")

        val thrownException = assertThrows<UnauthorizedException> { authService.login(request) }

        assertThat(thrownException).hasMessageThat().contains("Please verify your email before logging in.")
    }

    @Test
    fun `login returns auth response if credentials are valid and user is verified`() {
        createUser(email = "login-success@mail.com", isVerified = true, plainPassword = "Password@1")
        val request = LoginRequest(email = "login-success@mail.com", password = "Password@1")

        val authResponse = authService.login(request)

        assertThat(authResponse.accessToken).isEqualTo("access-token")
        assertThat(authResponse.refreshToken).isEqualTo("refresh-token")
        assertThat(refreshTokenRepository.findByToken("refresh-token")).isNotNull()
    }

    @Test
    fun `refreshToken throw UnauthorizedException if refresh token does not exist`() {
        val request = RefreshTokenRequest("missing-token")

        val thrownException = assertThrows<UnauthorizedException> { authService.refreshToken(request) }

        assertThat(thrownException).hasMessageThat().contains("Invalid refresh token")
    }

    @Test
    fun `refreshToken throw TokenExpiredException if refresh token is expired`() {
        val user = createUser(email = "expired-refresh@mail.com", isVerified = true)
        refreshTokenRepository.save(
            RefreshToken(token = "expired-refresh-token", expiryDate = LocalDateTime.now().minusSeconds(1), user = user)
        )
        val request = RefreshTokenRequest("expired-refresh-token")

        val thrownException = assertThrows<TokenExpiredException> { authService.refreshToken(request) }

        assertThat(thrownException).hasMessageThat().contains("Refresh token is expired. Please login again.")
    }

    @Test
    fun `refreshToken throw UnauthorizedException if jwt validation fails`() {
        val user = createUser(email = "invalid-jwt@mail.com", isVerified = true)
        refreshTokenRepository.save(
            RefreshToken(token = "invalid-jwt-token", expiryDate = LocalDateTime.now().plusDays(2), user = user)
        )
        every { jwtUtil.validateRefreshToken("invalid-jwt-token") } returns false
        val request = RefreshTokenRequest("invalid-jwt-token")

        val thrownException = assertThrows<UnauthorizedException> { authService.refreshToken(request) }

        assertThat(thrownException).hasMessageThat().contains("Invalid refresh token")
    }

    @Test
    fun `refreshToken throw UnauthorizedException if refresh token does not belong to user in jwt`() {
        val user = createUser(email = "token-user-mismatch@mail.com", isVerified = true)
        refreshTokenRepository.save(
            RefreshToken(token = "mismatch-token", expiryDate = LocalDateTime.now().plusDays(2), user = user)
        )
        every { jwtUtil.validateRefreshToken("mismatch-token") } returns true
        every { jwtUtil.validateTokenForUser("mismatch-token", user.id) } returns false
        val request = RefreshTokenRequest("mismatch-token")

        val thrownException = assertThrows<UnauthorizedException> { authService.refreshToken(request) }

        assertThat(thrownException).hasMessageThat().contains("Invalid refresh token")
        assertThat(refreshTokenRepository.findByToken("mismatch-token")?.token).isEqualTo("mismatch-token")
    }

    @Test
    fun `refreshToken returns new auth response if refresh token is valid`() {
        val user = createUser(email = "refresh-success@mail.com", isVerified = true)
        refreshTokenRepository.save(
            RefreshToken(token = "valid-refresh-token", expiryDate = LocalDateTime.now().plusDays(2), user = user)
        )
        every { jwtUtil.validateRefreshToken("valid-refresh-token") } returns true
        every { jwtUtil.validateTokenForUser("valid-refresh-token", user.id) } returns true
        val request = RefreshTokenRequest("valid-refresh-token")

        val authResponse = authService.refreshToken(request)

        assertThat(authResponse.accessToken).isEqualTo("access-token")
        assertThat(authResponse.refreshToken).isEqualTo("refresh-token")
        assertThat(refreshTokenRepository.findByToken("valid-refresh-token")).isNull()
        assertThat(refreshTokenRepository.findByToken("refresh-token")).isNotNull()
    }

    @Test
    fun `logout returns without exception if refresh token does not exist for user`() {
        val user = createUser(email = "logout-missing@mail.com", isVerified = true)
        val request = RefreshTokenRequest("missing-refresh-token")

        authService.logout(user.id, request)

        assertThat(refreshTokenRepository.findByToken("missing-refresh-token")).isNull()
    }

    @Test
    fun `logout returns without exception if refresh token exists for user`() {
        val user = createUser(email = "logout-success@mail.com", isVerified = true)
        refreshTokenRepository.save(
            RefreshToken(token = "logout-refresh-token", expiryDate = LocalDateTime.now().plusDays(1), user = user)
        )
        val request = RefreshTokenRequest("logout-refresh-token")

        authService.logout(user.id, request)

        assertThat(refreshTokenRepository.findByToken("logout-refresh-token")).isNull()
    }

    @Test
    fun `forgotPassword throw RuntimeException if account is not verified`() {
        createUser(email = "forgot-unverified@mail.com", isVerified = false)
        val request = ForgotPasswordRequest(email = "forgot-unverified@mail.com")

        val thrownException = assertThrows<RuntimeException> { authService.forgotPassword(request) }

        assertThat(thrownException).hasMessageThat()
            .contains("Account is not verified yet. Please verify your email first.")
    }

    @Test
    fun `forgotPassword returns success message if account is verified`() {
        val user = createUser(email = "forgot-verified@mail.com", isVerified = true)
        val request = ForgotPasswordRequest(email = user.email)

        val resultMessage = authService.forgotPassword(request)

        val savedOtp = emailVerificationRepository.findByOtpAndUser("12345", user)
        assertThat(resultMessage).isEqualTo("OTP sent successfully to your email.")
        assertThat(savedOtp).isNotNull()
    }

    @Test
    fun `verifyOtp throw RuntimeException if otp is invalid`() {
        createUser(email = "verify-otp-invalid@mail.com", isVerified = true)
        val request = VerifyOtpRequest(email = "verify-otp-invalid@mail.com", otp = "11111")

        val thrownException = assertThrows<RuntimeException> { authService.verifyOtp(request) }

        assertThat(thrownException).hasMessageThat().contains("Invalid or expired OTP")
    }

    @Test
    fun `verifyOtp throw RuntimeException if otp is expired`() {
        val user = createUser(email = "verify-otp-expired@mail.com", isVerified = true)
        emailVerificationRepository.save(
            EmailVerification(
                otp = "12345",
                sentAt = LocalDateTime.now().minusMinutes(20),
                user = user
            )
        )
        val request = VerifyOtpRequest(email = user.email, otp = "12345")

        val thrownException = assertThrows<RuntimeException> { authService.verifyOtp(request) }

        assertThat(thrownException).hasMessageThat().contains("OTP has expired")
    }

    @Test
    fun `verifyOtp returns success message if otp is valid`() {
        val user = createUser(email = "verify-otp-success@mail.com", isVerified = true)
        emailVerificationRepository.save(
            EmailVerification(
                otp = "12345",
                sentAt = LocalDateTime.now().minusMinutes(1),
                user = user
            )
        )
        val request = VerifyOtpRequest(email = user.email, otp = "12345")

        val resultMessage = authService.verifyOtp(request)

        assertThat(resultMessage).isEqualTo("OTP verified successfully. You can now reset your password.")
    }

    @Test
    fun `resetPassword throw RuntimeException if otp is invalid`() {
        createUser(email = "reset-invalid@mail.com", isVerified = true)
        val request = ResetPasswordRequest(email = "reset-invalid@mail.com", otp = "00000", newPassword = "Password@2")

        val thrownException = assertThrows<RuntimeException> { authService.resetPassword(request) }

        assertThat(thrownException).hasMessageThat().contains("Invalid OTP")
    }

    @Test
    fun `resetPassword throw RuntimeException if otp is expired`() {
        val user = createUser(email = "reset-expired@mail.com", isVerified = true)
        emailVerificationRepository.save(
            EmailVerification(
                otp = "12345",
                sentAt = LocalDateTime.now().minusMinutes(20),
                user = user
            )
        )
        val request = ResetPasswordRequest(email = user.email, otp = "12345", newPassword = "Password@2")

        val thrownException = assertThrows<RuntimeException> { authService.resetPassword(request) }

        assertThat(thrownException).hasMessageThat().contains("Invalid or expired OTP")
    }

    @Test
    fun `resetPassword returns success message if otp is valid`() {
        val user = createUser(email = "reset-success@mail.com", isVerified = true, plainPassword = "Password@1")
        emailVerificationRepository.save(
            EmailVerification(
                otp = "12345",
                sentAt = LocalDateTime.now().minusMinutes(1),
                user = user
            )
        )
        val request = ResetPasswordRequest(email = user.email, otp = "12345", newPassword = "Password@2")

        val resultMessage = authService.resetPassword(request)

        val updatedUser = userRepository.findByEmail(user.email)
        assertThat(resultMessage).isEqualTo("Password reset successfully. You can now login.")
        assertThat(passwordEncoder.matches("Password@2", updatedUser?.passwordHash)).isTrue()
    }

    @Test
    fun `resendOtp returns verification message if user is not verified`() {
        val user = createUser(email = "resend-unverified@mail.com", isVerified = false)
        val request = ForgotPasswordRequest(email = user.email)

        val resultMessage = authService.resendOtp(request)

        val savedOtp = emailVerificationRepository.findByOtpAndUser("12345", user)
        assertThat(resultMessage).isEqualTo("Verification OTP resent successfully to your email.")
        assertThat(savedOtp).isNotNull()
    }

    @Test
    fun `resendOtp returns password reset message if user is verified`() {
        val user = createUser(email = "resend-verified@mail.com", isVerified = true)
        val request = ForgotPasswordRequest(email = user.email)

        val resultMessage = authService.resendOtp(request)

        val savedOtp = emailVerificationRepository.findByOtpAndUser("12345", user)
        assertThat(resultMessage).isEqualTo("Password reset OTP resent successfully to your email.")
        assertThat(savedOtp).isNotNull()
    }

    @Test
    fun `clearExpiredRefreshTokens returns by deleting expired refresh tokens if any exists`() {
        val user = createUser(email = "clear-refresh@mail.com", isVerified = true)
        refreshTokenRepository.save(
            RefreshToken(
                token = "expired-a",
                expiryDate = LocalDateTime.now().minusDays(1),
                user = user
            )
        )
        refreshTokenRepository.save(
            RefreshToken(
                token = "active-a",
                expiryDate = LocalDateTime.now().plusDays(1),
                user = user
            )
        )

        authService.clearExpiredRefreshTokens()

        assertThat(refreshTokenRepository.findByToken("expired-a")).isNull()
        assertThat(refreshTokenRepository.findByToken("active-a")).isNotNull()
    }

    @Test
    fun `clearExpiredRefreshTokens keeps active refresh tokens when no token is expired`() {
        val user = createUser(email = "clear-refresh-none@mail.com", isVerified = true)
        refreshTokenRepository.save(
            RefreshToken(
                token = "active-only",
                expiryDate = LocalDateTime.now().plusDays(3),
                user = user
            )
        )

        authService.clearExpiredRefreshTokens()

        assertThat(refreshTokenRepository.findByToken("active-only")).isNotNull()
    }

    @Test
    fun `clearExpiredOtps returns by deleting expired otp records if any exists`() {
        val user = createUser(email = "clear-otp@mail.com", isVerified = true)
        emailVerificationRepository.save(
            EmailVerification(
                otp = "11111",
                sentAt = LocalDateTime.now().minusMinutes(20),
                user = user
            )
        )
        emailVerificationRepository.save(
            EmailVerification(
                otp = "22222",
                sentAt = LocalDateTime.now().minusMinutes(5),
                user = user
            )
        )

        authService.clearExpiredOtps()

        assertThat(emailVerificationRepository.findByOtpAndUser("11111", user)).isNull()
        assertThat(emailVerificationRepository.findByOtpAndUser("22222", user)).isNotNull()
    }

    @Test
    fun `clearExpiredOtps keeps recent otp records when no otp is expired`() {
        val user = createUser(email = "clear-otp-none@mail.com", isVerified = true)
        emailVerificationRepository.save(
            EmailVerification(
                otp = "33333",
                sentAt = LocalDateTime.now().minusMinutes(5),
                user = user
            )
        )

        authService.clearExpiredOtps()

        assertThat(emailVerificationRepository.findByOtpAndUser("33333", user)).isNotNull()
    }

    @Test
    fun `clearUnverifiedUsers returns by deleting stale unverified users if any exists`() {
        createUser(email = "clear-user-old@mail.com", isVerified = false, createdAt = LocalDateTime.now().minusDays(2))
        createUser(email = "clear-user-new@mail.com", isVerified = false, createdAt = LocalDateTime.now().minusHours(1))
        createUser(
            email = "clear-user-verified@mail.com",
            isVerified = true,
            createdAt = LocalDateTime.now().minusDays(2)
        )

        authService.clearUnverifiedUsers()

        assertThat(userRepository.findByEmail("clear-user-old@mail.com")).isNull()
        assertThat(userRepository.findByEmail("clear-user-new@mail.com")).isNotNull()
        assertThat(userRepository.findByEmail("clear-user-verified@mail.com")).isNotNull()
    }

    @Test
    fun `login throw EntityNotFoundException if user email does not exist`() {
        val request = LoginRequest(email = "missing-user@mail.com", password = "Password@1")

        val thrownException = assertThrows<EntityNotFoundException> { authService.login(request) }

        assertThat(thrownException).hasMessageThat().contains("User not found with this email")
    }

    private fun createUser(
        email: String,
        isVerified: Boolean,
        plainPassword: String = "Password@1",
        createdAt: LocalDateTime = LocalDateTime.now().minusHours(2)
    ): User {
        val userToSave = User(
            fullName = "Integration User",
            email = email,
            passwordHash = passwordEncoder.encode(plainPassword)
                ?: throw IllegalStateException("Password encoding failed"),
            gender = Gender.MALE,
            birthDate = LocalDate.of(1994, 5, 1),
            isVerified = isVerified,
            createdAt = createdAt
        )
        return userRepository.save(userToSave)
    }
}
