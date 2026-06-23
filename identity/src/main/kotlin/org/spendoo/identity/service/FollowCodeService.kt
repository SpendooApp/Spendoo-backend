package org.spendoo.identity.service

import org.spendoo.identity.entity.FollowCode
import org.spendoo.identity.repository.FollowCodeRepository
import org.spendoo.identity.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID


@Service
class FollowCodeService (
    private val followCodeRepository: FollowCodeRepository,
    private val userRepository : UserRepository,
){

    @Transactional
    fun generateCode(userId : UUID): String {

        val user = userRepository.findById(userId)
            .orElseThrow { RuntimeException("User not found") }

        var newCode: String
        do {
            newCode = generateRandomString(10)
        } while (followCodeRepository.existsByCode(newCode))

        val existingFollowCode = followCodeRepository.findByUserId(userId)

        if (existingFollowCode != null) {
            val updatedFollowCode = existingFollowCode.copy(code = newCode)
            followCodeRepository.save(updatedFollowCode)
        } else {
            val newFollowCode = FollowCode(
                user = user,
                code = newCode
            )
            followCodeRepository.save(newFollowCode)
        }
        return newCode
    }


    private fun generateRandomString(length: Int) : String {
        val allowedChars = ('0'..'9')
        return (1..length)
            .map { allowedChars.random() }
            .joinToString("")
    }

}