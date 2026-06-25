package org.spendoo.identity.integration

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.spendoo.identity.IdentityTestApplication
import org.spendoo.identity.entity.FollowCode
import org.spendoo.identity.entity.Gender
import org.spendoo.identity.entity.User
import org.spendoo.identity.repository.FollowCodeRepository
import org.spendoo.identity.repository.UserRepository
import org.spendoo.identity.service.FollowCodeService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

@SpringBootTest(classes = [IdentityTestApplication::class])
@ActiveProfiles("test")
class FollowCodeServiceIntegrationTest {

    @Autowired
    private lateinit var followCodeService: FollowCodeService

    @Autowired
    private lateinit var followCodeRepository: FollowCodeRepository

    @Autowired
    private lateinit var userRepository: UserRepository

    @BeforeEach
    fun setUp() {
        followCodeRepository.deleteAll()
        userRepository.deleteAll()
    }

    @Test
    fun `generateCode creates a new code if user does not have one`() {
        val user = createUser("new-code@mail.com")

        val generatedCode = followCodeService.generateCode(user.id)

        val savedCode = followCodeRepository.findByUserId(user.id)
        assertThat(generatedCode).hasLength(10)
        assertThat(savedCode).isNotNull()
        assertThat(savedCode?.code).isEqualTo(generatedCode)
    }

    @Test
    fun `generateCode updates existing code if user already has one`() {
        val user = createUser("existing-code@mail.com")
        val oldCode = FollowCode(user = user, code = "1234567890")
        followCodeRepository.save(oldCode)

        val newGeneratedCode = followCodeService.generateCode(user.id)

        val updatedCodeInDb = followCodeRepository.findByUserId(user.id)
        assertThat(newGeneratedCode).isNotEqualTo("1234567890")
        assertThat(updatedCodeInDb?.code).isEqualTo(newGeneratedCode)
    }

    @Test
    fun `generateCode throws Exception if user not found`() {
        val randomUserId = UUID.randomUUID()

        val thrownException = assertThrows<RuntimeException> {
            followCodeService.generateCode(randomUserId)
        }

        assertThat(thrownException).hasMessageThat().contains("User not found")
    }

    private fun createUser(email: String): User {
        return userRepository.save(
            User(
                fullName = "Test User",
                email = email,
                passwordHash = "hash",
                gender = Gender.FEMALE,
                birthDate = LocalDate.of(1998, 1, 1),
                isVerified = true,
                createdAt = LocalDateTime.now()
            )
        )
    }
}