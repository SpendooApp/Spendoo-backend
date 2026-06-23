package org.spendoo.identity.api.controller

import org.spendoo.identity.service.FollowCodeService
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID


@RestController
@RequestMapping("/api/v1/identity/follow-code")
class FollowCodeController (
    private val followService: FollowCodeService
){

    @PostMapping("/generate-code")
    fun generateOrRegenerateCode(@AuthenticationPrincipal userId: UUID): ResponseEntity<Map<String, String>> {
        val code = followService.generateCode(userId)
        return ResponseEntity.ok(mapOf("code" to code))

    }




}