package org.spendoo.identity.api.controller

import org.spendoo.identity.api.dto.response.FollowStatusResponse
import org.spendoo.identity.api.dto.response.UserSearchResponse
import org.spendoo.identity.service.FollowService
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID


@RestController
@RequestMapping("/api/v1/identity/follows")
class FollowController(
    private val followService: FollowService

) {
    @GetMapping("/search")
    fun searchUserByCode(@RequestParam code: String): ResponseEntity<UserSearchResponse> {
        return ResponseEntity.ok(followService.searchUserByCode(code))
    }

    @PostMapping("/request/{followeeId}")
    fun sendFollowRequest(
        @PathVariable followeeId: UUID,
        @AuthenticationPrincipal followerId: UUID
    ): ResponseEntity<Unit> {
        followService.sendFollowRequest(followerId, followeeId)
        return ResponseEntity.ok().build()
    }

    @PutMapping("/request/{followerId}/respond")
    fun respondToFollowRequest(
        @PathVariable followerId: UUID,
        @AuthenticationPrincipal followeeId: UUID,
        @RequestParam isApproved: Boolean
    ): ResponseEntity<Unit> {
        followService.respondToFollowRequest(followerId, followeeId, isApproved)
        return ResponseEntity.ok().build()
    }

    @GetMapping("/following")
    fun getFollowing(
        @AuthenticationPrincipal userId: UUID,
        pageable: Pageable
    ): ResponseEntity<Page<UserSearchResponse>> {
        return ResponseEntity.ok(followService.getFollowing(userId, pageable))
    }

    @GetMapping("/followers")
    fun getFollowers(
        @AuthenticationPrincipal userId: UUID,
        pageable: Pageable
    ): ResponseEntity<Page<UserSearchResponse>> {
        return ResponseEntity.ok(followService.getFollowers(userId, pageable))
    }

    @GetMapping("/requests")
    fun getPendingRequests(
        @AuthenticationPrincipal userId: UUID,
        pageable: Pageable
    ): ResponseEntity<Page<UserSearchResponse>> {
        return ResponseEntity.ok(followService.getPendingRequests(userId, pageable))
    }

    @DeleteMapping("/{followeeId}/unfollow")
    fun unfollowUser(
        @PathVariable followeeId: UUID,
        @AuthenticationPrincipal followerId: UUID
    ): ResponseEntity<Unit> {
        followService.removeFollowRelation(followerId, followeeId)
        return ResponseEntity.ok().build()
    }

    @DeleteMapping("/{followerId}/remove")
    fun removeFollower(
        @PathVariable followerId: UUID,
        @AuthenticationPrincipal followeeId: UUID
    ): ResponseEntity<Unit> {
        followService.removeFollowRelation(followerId, followeeId)
        return ResponseEntity.ok().build()
    }

    @GetMapping("/check-status")
    fun checkFollowStatus(
        @RequestParam followerId: UUID,
        @RequestParam followeeId: UUID
    ): ResponseEntity<FollowStatusResponse> {
        val response = followService.checkFollowStatus(followerId, followeeId)
        return ResponseEntity.ok(response)
    }
}