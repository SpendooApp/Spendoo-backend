package org.spendoo.identity.controller

import jakarta.validation.Valid
import org.spendoo.identity.dto.AuthResponse
import org.spendoo.identity.dto.LoginRequest
import org.spendoo.identity.dto.RefreshTokenRequest
import org.spendoo.identity.dto.RegisterRequest
import org.spendoo.identity.service.AuthService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController


@RestController
@RequestMapping("/api/auth")
class AuthController (
    private val authService: AuthService
) {

    @PostMapping("/signup")
    fun register (@Valid @RequestBody request: RegisterRequest): ResponseEntity<AuthResponse> {
        val response = authService.register(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
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

}