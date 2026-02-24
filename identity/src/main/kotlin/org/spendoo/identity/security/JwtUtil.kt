package org.spendoo.identity.security

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.util.Base64
import java.util.Date
import javax.crypto.SecretKey
import java.time.Duration

@Component
class JwtUtil(
    @Value("\${jwt.secret-key}") private val secret: String
){

    private val secretKey: SecretKey by lazy {
        Keys.hmacShaKeyFor(Base64.getDecoder().decode(secret))
    }
    private val accessExpiration = Duration.ofMinutes(30)
    private val refreshExpiration = Duration.ofDays(14)

    // ================== Generate ==================

    private fun createToken(email: String, expirationTimeMillis: Long, type: String): String {
        return Jwts.builder()
            .setSubject(email)
            .claim("type", type)
            .setIssuedAt(Date(System.currentTimeMillis()))
            .setExpiration(Date(System.currentTimeMillis() + expirationTimeMillis))
            .signWith(secretKey)
            .compact()
    }

    fun generateAccessToken(email: String): String {
        return createToken(email, accessExpiration.toMillis(), "access")
    }

    fun generateRefreshToken(email: String): String {
        return createToken(email, refreshExpiration.toMillis(), "refresh")
    }

    // ================== Parsing ==================

    private fun parseAllClaims(token: String) =
        try {
            Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .body
        } catch (e: Exception) {
            null
        }

    fun extractUsername(token: String): String? {
        return parseAllClaims(token)?.subject
    }

    // ================== Validation ==================

    private fun isTokenExpired(claims: Claims): Boolean {
        return claims.expiration.before(Date())
    }

    private fun validateTokenInternal(token: String): Claims? {
        val claims = parseAllClaims(token) ?: return null
        if (isTokenExpired(claims)) return null
        return claims
    }

    fun validateAccessToken(token: String): Boolean {
        val claims = validateTokenInternal(token) ?: return false
        return claims["type"] == "access"
    }

    fun validateRefreshToken(token: String): Boolean {
        val claims = validateTokenInternal(token) ?: return false
        return claims["type"] == "refresh"
    }

    fun validateTokenForUser(token: String, username: String): Boolean {
        val claims = validateTokenInternal(token) ?: return false
        return claims.subject == username
    }

}

