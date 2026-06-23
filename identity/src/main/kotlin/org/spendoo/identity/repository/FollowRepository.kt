package org.spendoo.identity.repository

import org.spendoo.identity.entity.Follow
import org.spendoo.identity.entity.FollowStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface FollowRepository: JpaRepository<Follow, UUID> {

    fun existsByFollowerIdAndFolloweeId(followerId: UUID, followeeId: UUID): Boolean


    fun findByFollowerIdAndStatus(followerId: UUID, status: FollowStatus, pageable: Pageable): Page<Follow>


    fun findByFolloweeIdAndStatus(followeeId: UUID, status: FollowStatus, pageable: Pageable): Page<Follow>

    fun findByFollowerIdAndFolloweeId(followerId: UUID, followeeId: UUID): Follow?
}

