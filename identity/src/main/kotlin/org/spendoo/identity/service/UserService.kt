package org.spendoo.identity.service

import org.spendoo.events.publisher.SpendooEventPublisher
import org.spendoo.identity.entity.User
import org.spendoo.identity.exception.UserNotFoundException
import org.spendoo.identity.repository.UserRepository
import org.spendoo.identity.service.mapper.toUserUpdatedEvent
import org.spendoo.storage.service.ImageStorageService
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.util.*

@Service
class UserService(
    private val userRepository: UserRepository,
    private val imageStorageService: ImageStorageService,
    private val eventPublisher: SpendooEventPublisher,
    @param:Value("\${identity.resources.profile-image-directory}") private val profileImageDirectory: String
) {
    fun existById(userId: UUID): Boolean = userRepository.existsById(userId)

    fun findById(userId: UUID): User {
        return userRepository.findByIdOrNull(userId)
            ?: throw UserNotFoundException("User with id: $userId not found")
    }

    fun updateUserImage(
        userId: UUID,
        imageFile: MultipartFile,
    ): String {
        val user = findById(userId)
        val newImageUrl = imageStorageService.uploadImage(
            file = imageFile,
            fileName = "${user.id}",
            folderName = profileImageDirectory
        )
        val savedUser = userRepository.save(user.copy(imageUrl = newImageUrl))
        eventPublisher.publish(savedUser.toUserUpdatedEvent())
        return newImageUrl
    }

    fun deleteUserImage(userId: UUID) {
        val user = findById(userId)
        user.imageUrl?.let { imageUrl ->
            imageStorageService.deleteImage(
                fileName = imageUrl,
                folderName = profileImageDirectory
            )
            val savedUser = userRepository.save(user.copy(imageUrl = null))
            eventPublisher.publish(savedUser.toUserUpdatedEvent())
        }
    }
}