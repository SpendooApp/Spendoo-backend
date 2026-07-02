package org.spendoo.identity.api.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.spendoo.identity.api.dto.request.*
import org.spendoo.identity.api.dto.response.AuthResponse
import org.spendoo.identity.service.AuthService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import java.util.*


@RestController
@RequestMapping("/api/v1/identity/auth")
class AuthController (
    private val authService: AuthService
) {

    @Tag(name = "Registration", description = "Endpoints related to user registration and account verification")
    @PostMapping("/signup")
    fun register (@Valid @RequestBody request: RegisterRequest): ResponseEntity<String> {
        val response = authService.register(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }

    @Tag(name = "Registration")
    @PostMapping("/verify-account")
    fun verifyAccount(@Valid @RequestBody request: VerifyOtpRequest): ResponseEntity<AuthResponse> {
        val response = authService.verifyAccount(request)
        return ResponseEntity.ok(response)

    }

    @Tag(name = "Authentication", description = "Endpoints related to user login, token refresh, and logout")
    @PostMapping("/login")
    fun login (@Valid @RequestBody request: LoginRequest): ResponseEntity<AuthResponse> {
        val response = authService.login(request)
        return ResponseEntity.ok(response)
    }

    @Tag(name = "Authentication")
    @PostMapping("/refresh")
    fun refresh (@Valid @RequestBody request: RefreshTokenRequest): ResponseEntity<AuthResponse> {
        val response = authService.refreshToken(request)
        return ResponseEntity.ok(response)
    }

    @Tag(name = "Authentication")
    @PostMapping("/logout")
    fun logout(
        @AuthenticationPrincipal userId: UUID,
        @Valid @RequestBody request: RefreshTokenRequest
    ): ResponseEntity<Map<String, String>> {
        authService.logout(userId, request)
        return ResponseEntity.ok(mapOf("message" to "Logged out successfully"))
    }

    @Tag(name = "Authentication")
    @PatchMapping("/device-token")
    fun updateDeviceToken(
        @AuthenticationPrincipal userId: UUID,
        @Valid @RequestBody request: UpdateDeviceTokenRequest
    ): ResponseEntity<Unit> {
        authService.updateDeviceToken(userId, request.refreshToken, request.deviceToken)
        return ResponseEntity.ok().build()
    }

    @Tag(name = "Password Management", description = "Endpoints related to password reset and OTP verification")
    @PostMapping("/forgot-password")
    fun forgotPassword (@Valid @RequestBody request: ForgotPasswordRequest): ResponseEntity<String> {
        val response = authService.forgotPassword(request)
        return ResponseEntity.ok(response)
    }

    @Tag(name = "Password Management")
    @PostMapping("/verify-otp")
    fun verifyOtp(@Valid @RequestBody request: VerifyOtpRequest): ResponseEntity<String> {
        val response = authService.verifyOtp(request)
        return ResponseEntity.ok(response)
    }

    @Tag(name = "Password Management")
    @PostMapping("/reset-password")
    fun resetPassword (@Valid @RequestBody request: ResetPasswordRequest): ResponseEntity<String> {
        val response = authService.resetPassword(request)
        return ResponseEntity.ok(response)
    }

    @Operation(tags = ["Password Management", "Registration"], summary = "Resend OTP for account verification or password reset")
    @PostMapping("/resend-otp")
    fun resendOtp(@Valid @RequestBody request: ForgotPasswordRequest): ResponseEntity<String> {
        val response = authService.resendOtp(request)
        return ResponseEntity.ok(response)
    }
}