package org.spendoo.identity.service

import org.spendoo.identity.repository.UserRepository
import org.springframework.stereotype.Service
import java.util.*

@Service
class UserService(
    private val userRepository: UserRepository,
) {
    fun existById(userId: UUID): Boolean = userRepository.existsById(userId)
}