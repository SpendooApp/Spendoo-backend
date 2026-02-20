package org.spendoo.identity.security

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.util.Base64
import java.util.Date
import java.time.Instant
import javax.crypto.SecretKey
import java.time.Duration

@Component
class JwtUtil(
    @Value("\${jwt.secret-key}") private val secret: String
){

    private val accessExpiration: Duration = Duration.ofHours(1)
    private val secretKey: SecretKey by lazy {
        Keys.hmacShaKeyFor(Base64.getDecoder().decode(secret))
    }

    fun generateToken(username: String): String {
        return Jwts.builder()
            .setSubject(username)
            .setIssuedAt(Date())
            .setExpiration(Date.from(Instant.now().plus(accessExpiration)))
            .signWith(secretKey)
            .compact()
    }

    fun extractUsername(token: String): String? {
        return try {
            Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .body
                .subject
        } catch (e: Exception) {
            null
        }
    }


    fun validateToken(token: String, username: String): Boolean {
        val extracted = extractUsername(token)
        return extracted == username && !isTokenExpired(token)
    }

    private fun isTokenExpired(token: String): Boolean {
        return try {
            val expiration = Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .body
                .expiration
            expiration.before(Date())
        } catch (e: Exception) {
            true
        }
    }
}

