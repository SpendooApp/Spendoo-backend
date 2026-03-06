package org.spendoo.identity.api.controller

import org.spendoo.identity.api.dto.response.UpdateImageResponse
import org.spendoo.identity.service.UserService
import org.spendoo.identity.service.mapper.toProfileResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.util.*

@RestController
@RequestMapping("/api/v1/identity/profile")
class ProfileController(
    private val userService: UserService,
    @Value("\${storage.spendoo.cdn-endpoint}") cdnEndpoint: String,
    @Value("\${identity.resources.profile-image-directory}") profileImageDirectory: String,
) {
    private val imagesBaseUrl: String = "$cdnEndpoint$profileImageDirectory"

    @GetMapping
    fun getUserProfile(@AuthenticationPrincipal userId: UUID): ResponseEntity<ProfileResponse> {
        val response = userService.findById(userId).toProfileResponse(imagesBaseUrl)
        return ResponseEntity.ok(response)
    }

    @PatchMapping(path = ["/image"], consumes = ["multipart/form-data"])
    fun updateUserImage(
        @AuthenticationPrincipal userId: UUID,
        @RequestPart("file") file: MultipartFile,
    ): ResponseEntity<UpdateImageResponse> {
        val imageUri = userService.updateUserImage(userId, file)
        val response = UpdateImageResponse(imageUrl = "$imagesBaseUrl/$imageUri")
        return ResponseEntity.ok(response)
    }

    @DeleteMapping("/image")
    fun deleteUserImage(@AuthenticationPrincipal userId: UUID): ResponseEntity<Unit> {
        userService.deleteUserImage(userId)
        return ResponseEntity.ok().build()
    }
}