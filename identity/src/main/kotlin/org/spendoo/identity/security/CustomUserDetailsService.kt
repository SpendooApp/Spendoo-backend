package org.spendoo.identity.security

import org.spendoo.identity.repository.UserRepository
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.stereotype.Service

@Service
class CustomUserDetailsService(
    private val userRepository: UserRepository
) : UserDetailsService {

    override fun loadUserByUsername(email: String): UserDetails {
        val user = userRepository.findByEmail(email)
            .orElseThrow {UsernameNotFoundException("User not found with email: $email") }

        return User.builder()
            .username(user.email)
            .password(user.passwordHash)
            .roles("USER")
            .build()
    }
}