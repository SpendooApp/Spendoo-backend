package org.spendoo.identity.integration

import com.google.common.truth.Truth.assertThat
import io.mockk.every
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.spendoo.events.publisher.SpendooEventPublisher
import org.spendoo.identity.IdentityTestApplication
import org.spendoo.identity.entity.Gender
import org.spendoo.identity.entity.User
import org.spendoo.identity.exception.UserNotFoundException
import org.spendoo.identity.repository.UserRepository
import org.spendoo.identity.service.UserService
import org.spendoo.storage.service.ImageStorageService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.context.ActiveProfiles
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

@SpringBootTest(classes = [IdentityTestApplication::class])
@ActiveProfiles("test")
class UserServiceIntegrationTest {

    @Autowired
    private lateinit var userService: UserService

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var imageStorageService: ImageStorageService

    @Autowired
    private lateinit var spendooEventPublisher: SpendooEventPublisher

    @BeforeEach
    fun setUp() {
        userRepository.deleteAll()
    }

    @Test
    fun `existById returns true if user exists`() {
        val existingUser = createUser(email = "exist-true@mail.com")

        val userExists = userService.existById(existingUser.id)

        assertThat(userExists).isTrue()
    }

    @Test
    fun `existById returns false if user does not exist`() {
        val missingUserId = UUID.randomUUID()

        val userExists = userService.existById(missingUserId)

        assertThat(userExists).isFalse()
    }

    @Test
    fun `findById returns user if user exists`() {
        val existingUser = createUser(email = "find-user@mail.com")

        val foundUser = userService.findById(existingUser.id)

        assertThat(foundUser.id).isEqualTo(existingUser.id)
        assertThat(foundUser.email).isEqualTo(existingUser.email)
    }

    @Test
    fun `findById throw UserNotFoundException if user does not exist`() {
        val missingUserId = UUID.randomUUID()

        val thrownException = assertThrows<UserNotFoundException> { userService.findById(missingUserId) }

        assertThat(thrownException).hasMessageThat().contains("User with id: $missingUserId not found")
    }

    @Test
    fun `updateUserImage returns uploaded image url if user exists`() {
        val existingUser = createUser(email = "image-update@mail.com")
        every {
            imageStorageService.uploadImage(any(), eq(existingUser.id.toString()), eq("test-profile-images"))
        } returns "updated-image.jpg?time=test"
        val imageFile = MockMultipartFile("file", "profile.jpg", "image/jpeg", "file-content".toByteArray())

        val returnedImageUrl = userService.updateUserImage(existingUser.id, imageFile)

        val updatedUser = userRepository.findByEmail(existingUser.email)
        assertThat(returnedImageUrl).isEqualTo("updated-image.jpg?time=test")
        assertThat(updatedUser?.imageUrl).isEqualTo("updated-image.jpg?time=test")
    }

    @Test
    fun `updateUserImage throw UserNotFoundException if user does not exist`() {
        val missingUserId = UUID.randomUUID()
        val imageFile = MockMultipartFile("file", "profile.jpg", "image/jpeg", "file-content".toByteArray())

        val thrownException =
            assertThrows<UserNotFoundException> { userService.updateUserImage(missingUserId, imageFile) }

        assertThat(thrownException).hasMessageThat().contains("User with id: $missingUserId not found")
    }

    @Test
    fun `deleteUserImage returns by removing image if user has image`() {
        val existingUser = createUser(email = "delete-image@mail.com", imageUrl = "existing-image.jpg?time=test")

        userService.deleteUserImage(existingUser.id)

        val updatedUser = userRepository.findByEmail(existingUser.email)
        assertThat(updatedUser?.imageUrl).isNull()
    }

    @Test
    fun `deleteUserImage returns without changes if user has no image`() {
        val existingUser = createUser(email = "delete-no-image@mail.com", imageUrl = null)

        userService.deleteUserImage(existingUser.id)

        val userAfterDelete = userRepository.findByEmail(existingUser.email)
        assertThat(userAfterDelete?.imageUrl).isNull()
    }

    @Test
    fun `deleteUserImage throw UserNotFoundException if user does not exist`() {
        val missingUserId = UUID.randomUUID()

        val thrownException = assertThrows<UserNotFoundException> { userService.deleteUserImage(missingUserId) }

        assertThat(thrownException).hasMessageThat().contains("User with id: $missingUserId not found")
    }

    private fun createUser(email: String, imageUrl: String? = null): User {
        return userRepository.save(
            User(
                fullName = "Image User",
                email = email,
                passwordHash = "encoded-password",
                gender = Gender.FEMALE,
                birthDate = LocalDate.of(1996, 2, 2),
                isVerified = true,
                createdAt = LocalDateTime.now().minusDays(2),
                imageUrl = imageUrl
            )
        )
    }
}
