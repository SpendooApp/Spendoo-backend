package org.spendoo.identity.controller

import jakarta.validation.Valid
import org.spendoo.identity.dto.request.ForgotPasswordRequest
import org.spendoo.identity.dto.response.AuthResponse
import org.spendoo.identity.dto.request.LoginRequest
import org.spendoo.identity.dto.request.RefreshTokenRequest
import org.spendoo.identity.dto.request.RegisterRequest
import org.spendoo.identity.dto.request.ResetPasswordRequest
import org.spendoo.identity.dto.request.VerifyOtpRequest
import org.spendoo.identity.service.AuthService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController


@RestController
@RequestMapping("/api/v1/auth")
class AuthController (
    private val authService: AuthService
) {

    @PostMapping("/signup")
    fun register (@Valid @RequestBody request: RegisterRequest): ResponseEntity<String> {
        val response = authService.register(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }

    @PostMapping("/verify-account")
    fun verifyAccount(@Valid @RequestBody request: VerifyOtpRequest): ResponseEntity<AuthResponse> {
        val response = authService.verifyAccount(request)
        return ResponseEntity.ok(response)

    }

    @PostMapping("/login")
    fun login (@Valid @RequestBody request: LoginRequest): ResponseEntity<AuthResponse> {
        val response = authService.login(request)
        return ResponseEntity.ok(response)
    }

    @PostMapping("/refresh")
    fun refresh (@Valid @RequestBody request: RefreshTokenRequest): ResponseEntity<AuthResponse> {
        val response = authService.refreshToken(request)
        return ResponseEntity.ok(response)
    }

    @PostMapping("/logout")
    fun logout(@Valid @RequestBody request: RefreshTokenRequest): ResponseEntity<Map<String, String>> {
        authService.logout(request)
        return ResponseEntity.ok(mapOf("message" to "Logged out successfully"))
    }


    @PostMapping("/forgot-password")
    fun forgotPassword (@Valid @RequestBody request: ForgotPasswordRequest): ResponseEntity<String> {
        val response = authService.forgotPassword(request)
        return ResponseEntity.ok(response)
    }

    @PostMapping("/verify-otp")
    fun verifyOtp(@Valid @RequestBody request: VerifyOtpRequest): ResponseEntity<String> {
        val response = authService.verifyOtp(request)
        return ResponseEntity.ok(response)
    }

    @PostMapping("/reset-password")
    fun resetPassword (@Valid @RequestBody request: ResetPasswordRequest): ResponseEntity<String> {
        val response = authService.resetPassword(request)
        return ResponseEntity.ok(response)
    }

    @PostMapping("/resend-otp")
    fun resendOtp(@Valid @RequestBody request: ForgotPasswordRequest): ResponseEntity<String> {
        val response = authService.resendOtp(request)
        return ResponseEntity.ok(response)
    }







}