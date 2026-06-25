package org.spendoo.identity.integration

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.spendoo.identity.IdentityTestApplication
import org.spendoo.identity.entity.Follow
import org.spendoo.identity.entity.FollowCode
import org.spendoo.identity.entity.FollowStatus
import org.spendoo.identity.entity.Gender
import org.spendoo.identity.entity.User
import org.spendoo.identity.repository.FollowCodeRepository
import org.spendoo.identity.repository.FollowRepository
import org.spendoo.identity.repository.UserRepository
import org.spendoo.identity.service.FollowService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.domain.PageRequest
import org.springframework.test.context.ActiveProfiles
import java.time.LocalDate
import java.time.LocalDateTime

@SpringBootTest(classes = [IdentityTestApplication::class])
@ActiveProfiles("test")
class FollowServiceIntegrationTest {

    @Autowired
    private lateinit var followService: FollowService

    @Autowired
    private lateinit var followRepository: FollowRepository

    @Autowired
    private lateinit var followCodeRepository: FollowCodeRepository

    @Autowired
    private lateinit var userRepository: UserRepository

    @BeforeEach
    fun setUp() {
        followRepository.deleteAll()
        followCodeRepository.deleteAll()
        userRepository.deleteAll()
    }

    @Test
    fun `searchUserByCode returns user if code is valid`() {
        val user = createUser("search@mail.com", "Target User")
        followCodeRepository.save(FollowCode(user = user, code = "VALID12345"))

        val response = followService.searchUserByCode("VALID12345")

        assertThat(response.userId).isEqualTo(user.id)
        assertThat(response.fullName).isEqualTo("Target User")
    }

    @Test
    fun `searchUserByCode throws Exception if code is invalid`() {
        val thrownException = assertThrows<RuntimeException> {
            followService.searchUserByCode("INVALID000")
        }
        assertThat(thrownException).hasMessageThat().contains("Code not found or invalid")
    }

    @Test
    fun `sendFollowRequest saves pending request successfully`() {
        val follower = createUser("follower@mail.com", "Follower")
        val followee = createUser("followee@mail.com", "Followee")

        followService.sendFollowRequest(follower.id, followee.id)

        val savedFollow = followRepository.findByFollowerIdAndFolloweeId(follower.id, followee.id)
        assertThat(savedFollow).isNotNull()
        assertThat(savedFollow?.status).isEqualTo(FollowStatus.PENDING)
    }

    @Test
    fun `sendFollowRequest throws Exception if following self`() {
        val user = createUser("self@mail.com", "Self")

        val thrownException = assertThrows<IllegalArgumentException> {
            followService.sendFollowRequest(user.id, user.id)
        }
        assertThat(thrownException).hasMessageThat().contains("You cannot follow yourself")
    }

    @Test
    fun `respondToFollowRequest updates status to ACCEPTED if approved`() {
        val follower = createUser("follower@mail.com", "Follower")
        val followee = createUser("followee@mail.com", "Followee")
        createFollowRelation(follower, followee, FollowStatus.PENDING)

        followService.respondToFollowRequest(follower.id, followee.id, isApproved = true)

        val updatedFollow = followRepository.findByFollowerIdAndFolloweeId(follower.id, followee.id)
        assertThat(updatedFollow?.status).isEqualTo(FollowStatus.ACCEPTED)
    }

    @Test
    fun `respondToFollowRequest deletes request if rejected`() {
        val follower = createUser("follower@mail.com", "Follower")
        val followee = createUser("followee@mail.com", "Followee")
        createFollowRelation(follower, followee, FollowStatus.PENDING)

        followService.respondToFollowRequest(follower.id, followee.id, isApproved = false)

        val deletedFollow = followRepository.findByFollowerIdAndFolloweeId(follower.id, followee.id)
        assertThat(deletedFollow).isNull()
    }

    @Test
    fun `getFollowing returns paginated list of accepted followees`() {
        val user = createUser("user@mail.com", "User")
        val followee = createUser("followee@mail.com", "Target")
        createFollowRelation(user, followee, FollowStatus.ACCEPTED)

        val result = followService.getFollowing(user.id, PageRequest.of(0, 10))

        assertThat(result.content).hasSize(1)
        assertThat(result.content[0].userId).isEqualTo(followee.id)
    }

    @Test
    fun `getFollowers returns paginated list of accepted followers`() {
        val targetUser = createUser("target@mail.com", "Target")
        val follower = createUser("follower@mail.com", "Follower")
        createFollowRelation(follower, targetUser, FollowStatus.ACCEPTED)

        val result = followService.getFollowers(targetUser.id, PageRequest.of(0, 10))

        assertThat(result.content).hasSize(1)
        assertThat(result.content[0].userId).isEqualTo(follower.id)
    }

    @Test
    fun `removeFollowRelation deletes the follow record`() {
        val follower = createUser("follower@mail.com", "Follower")
        val followee = createUser("followee@mail.com", "Followee")
        createFollowRelation(follower, followee, FollowStatus.ACCEPTED)

        followService.removeFollowRelation(follower.id, followee.id)

        val deletedFollow = followRepository.findByFollowerIdAndFolloweeId(follower.id, followee.id)
        assertThat(deletedFollow).isNull()
    }

    @Test
    fun `checkFollowStatus returns true if accepted`() {
        val follower = createUser("follower@mail.com", "Follower")
        val followee = createUser("followee@mail.com", "Followee")
        createFollowRelation(follower, followee, FollowStatus.ACCEPTED)

        val isFollowing = followService.checkFollowStatus(follower.id, followee.id)

        assertThat(isFollowing).isTrue()
    }

    @Test
    fun `checkFollowStatus returns false if pending or not found`() {
        val follower = createUser("follower@mail.com", "Follower")
        val followee = createUser("followee@mail.com", "Followee")
        createFollowRelation(follower, followee, FollowStatus.PENDING)

        val isFollowing = followService.checkFollowStatus(follower.id, followee.id)

        assertThat(isFollowing).isFalse()
    }

    @Test
    fun `checkFollowStatus returns true if checking own status`() {
        val user = createUser("self@mail.com", "Self")

        val isFollowing = followService.checkFollowStatus(user.id, user.id)

        assertThat(isFollowing).isTrue()
    }

    private fun createUser(email: String, fullName: String): User {
        return userRepository.save(
            User(
                fullName = fullName,
                email = email,
                passwordHash = "hash",
                gender = Gender.FEMALE,
                birthDate = LocalDate.of(1998, 1, 1),
                isVerified = true,
                createdAt = LocalDateTime.now()
            )
        )
    }

    private fun createFollowRelation(follower: User, followee: User, status: FollowStatus): Follow {
        return followRepository.save(
            Follow(
                follower = follower,
                followee = followee,
                status = status,
                createdAt = LocalDateTime.now()
            )
        )
    }
}