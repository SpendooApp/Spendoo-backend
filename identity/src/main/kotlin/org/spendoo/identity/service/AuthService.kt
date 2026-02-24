package org.spendoo.identity.service

import org.spendoo.identity.dto.response.AuthResponse
import org.spendoo.identity.dto.request.LoginRequest
import org.spendoo.identity.dto.request.RefreshTokenRequest
import org.spendoo.identity.dto.request.RegisterRequest
import org.spendoo.identity.entity.RefreshToken
import org.spendoo.identity.entity.User
import org.spendoo.identity.exception.TokenExpiredException
import org.spendoo.identity.exception.UnauthorizedException
import org.spendoo.identity.exception.UserAlreadyExistsException
import org.spendoo.identity.mapper.toEntity
import org.spendoo.identity.repository.RefreshTokenRepository
import org.spendoo.identity.repository.UserRepository
import org.spendoo.identity.security.JwtUtil
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtUtil: JwtUtil,
    private val authenticationManager: AuthenticationManager
) {

    fun register(request: RegisterRequest): AuthResponse {
        if (userRepository.findByEmail(request.email).isPresent) {
            throw UserAlreadyExistsException("Email is already registered!")
        }


        val user = request.toEntity(passwordEncoder.encode(request.password)!!)

        userRepository.save(user)

        val accessToken = jwtUtil.generateAccessToken(user.email)
        val refreshToken = jwtUtil.generateRefreshToken(user.email)

        saveRefreshToken(user, refreshToken)

        return AuthResponse(accessToken, refreshToken)
    }


    fun login(request: LoginRequest): AuthResponse {
        authenticationManager.authenticate(
            UsernamePasswordAuthenticationToken(request.email, request.password)
        )

        val user = userRepository.findByEmail(request.email).get()
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

    fun logout(request: RefreshTokenRequest) {
        refreshTokenRepository.findByToken(request.refreshToken).ifPresent { tokenEntity ->
            refreshTokenRepository.delete(tokenEntity)
        }
    }
}