package org.spendoo.identity.service

import org.spendoo.identity.api.dto.response.UserSearchResponse
import org.spendoo.identity.entity.Follow
import org.spendoo.identity.entity.FollowStatus
import org.spendoo.identity.repository.FollowCodeRepository
import org.spendoo.identity.repository.FollowRepository
import org.spendoo.identity.repository.UserRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.UUID

@Service
class FollowService (

    private val followRepository: FollowRepository,
    private val followCodeRepository: FollowCodeRepository,
    private val userRepository: UserRepository
) {

    @Transactional(readOnly = true)
    fun searchUserByCode(code: String): UserSearchResponse {
        val followCode = followCodeRepository.findByCode(code)
            ?: throw RuntimeException("Code not found or invalid")

        val user = followCode.user

        return UserSearchResponse(
            userId = user.id,
            fullName = user.fullName,
            imageUrl = user.imageUrl
        )
    }

    @Transactional
    fun sendFollowRequest(followerId: UUID, followeeId: UUID) {
        if (followerId == followeeId) {
            throw IllegalArgumentException("You cannot follow yourself")
        }

        val follower = userRepository.findById(followerId)
            .orElseThrow { RuntimeException("Follower not found") }
        val followee = userRepository.findById(followeeId)
            .orElseThrow { RuntimeException("Followee not found") }

        if (followRepository.existsByFollowerIdAndFolloweeId(followerId, followeeId)) {
            throw IllegalStateException("Follow request already exists")
        }

        val followRequest = Follow(
            follower = follower,
            followee = followee,
            status = FollowStatus.PENDING,
            createdAt = LocalDateTime.now()
        )
        followRepository.save(followRequest)
    }

    @Transactional
    fun respondToFollowRequest(followerId: UUID, followeeId: UUID, isApproved: Boolean) {

        val followRequest = followRepository.findByFollowerIdAndFolloweeId(followerId, followeeId)
            ?: throw RuntimeException("Follow request not found")

        if (followRequest.status != FollowStatus.PENDING) {
            throw RuntimeException("Request is not in pending state")
        }

        if (isApproved) {
            val updatedRequest = followRequest.copy(status = FollowStatus.ACCEPTED)
            followRepository.save(updatedRequest)
        } else {
            followRepository.delete(followRequest)
        }
    }

    @Transactional(readOnly = true)
    fun getFollowing(userId: UUID, pageable: Pageable): Page<UserSearchResponse> {
        val follows = followRepository.findByFollowerIdAndStatus(userId, FollowStatus.ACCEPTED, pageable)

        return follows.map {
            UserSearchResponse(it.followee.id, it.followee.fullName, it.followee.imageUrl)
        }
    }
    @Transactional(readOnly = true)
    fun getFollowers(userId: UUID, pageable: Pageable): Page<UserSearchResponse> {

        val follows = followRepository.findByFolloweeIdAndStatus(userId, FollowStatus.ACCEPTED, pageable)


        return follows.map {
            UserSearchResponse(it.follower.id, it.follower.fullName, it.follower.imageUrl)
        }
    }

    @Transactional(readOnly = true)
    fun getPendingRequests(userId: UUID, pageable: Pageable): Page<UserSearchResponse> {

        val requests = followRepository.findByFolloweeIdAndStatus(userId, FollowStatus.PENDING, pageable)

        return requests.map {
            UserSearchResponse(
                userId = it.follower.id,
                fullName = it.follower.fullName,
                imageUrl = it.follower.imageUrl,
            )
        }
    }

    @Transactional
    fun removeFollowRelation(followerId: UUID, followeeId: UUID) {

        val follow = followRepository.findByFollowerIdAndFolloweeId(followerId, followeeId)
            ?: throw RuntimeException("Follow relationship not found")

        followRepository.delete(follow)
    }

    @Transactional(readOnly = true)
    fun checkFollowStatus(followerId: UUID, followeeId: UUID): Boolean {

        if(followerId == followeeId)
            return true

        val follow = followRepository.findByFollowerIdAndFolloweeId(followerId, followeeId)

        return follow != null && follow.status == FollowStatus.ACCEPTED

    }




}