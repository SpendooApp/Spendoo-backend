package org.spendoo.identity.service

import org.spendoo.events.publisher.SpendooEventPublisher
import org.spendoo.identity.api.dto.request.UpdateProfileRequest
import org.spendoo.identity.api.dto.response.ProfileResponse
import org.spendoo.identity.entity.PlanCode
import org.spendoo.identity.entity.User
import org.spendoo.identity.exception.UserNotFoundException
import org.spendoo.identity.repository.FollowCodeRepository
import org.spendoo.identity.repository.UserRepository
import org.spendoo.identity.repository.UserSubscriptionRepository
import org.spendoo.identity.service.mapper.toProfileResponse
import org.spendoo.identity.service.mapper.toUserUpdatedEvent
import org.spendoo.storage.service.ImageStorageService
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.util.*

@Service
class UserService(
    private val userRepository: UserRepository,
    private val imageStorageService: ImageStorageService,
    private val eventPublisher: SpendooEventPublisher,
    private val userSubscriptionRepository: UserSubscriptionRepository,
    private val followCodeRepository: FollowCodeRepository,
    private val followCodeService: FollowCodeService,
    @param:Value("\${identity.resources.profile-image-directory}") private val profileImageDirectory: String
) {
    fun existById(userId: UUID): Boolean = userRepository.existsById(userId)

    fun findById(userId: UUID): User {
        return userRepository.findByIdOrNull(userId)
            ?: throw UserNotFoundException("User with id: $userId not found")
    }

    @Transactional
    fun getUserProfile(userId: UUID, imageBaseUrl: String, languageCode:String): ProfileResponse {
        val user = findById(userId)
        val subscription = userSubscriptionRepository.findByUserId(userId)

        val followCodeEntity = followCodeRepository.findByUserId(userId)
        val activeFollowCode = followCodeEntity?.code ?: followCodeService.generateCode(userId).code

        val useArabic = languageCode.lowercase().startsWith("ar")
        val planTitle = if (useArabic) {
            subscription?.subscriptionPlan?.titleAr ?: "مجاني"
        } else {
            subscription?.subscriptionPlan?.titleEn ?: "Free"
        }
        val planCode = subscription?.subscriptionPlan?.code ?:PlanCode.FREE
        return user.toProfileResponse(imageBaseUrl, planTitle, planCode, activeFollowCode)
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

    fun updateProfile(userId: UUID, request: UpdateProfileRequest) {
        val user = findById(userId)

        val updatedUser = user.copy(
            fullName = request.fullName,
            gender = request.gender,
            birthDate = request.birthDate,
        )

        val savedUser = userRepository.save(updatedUser)
        eventPublisher.publish(savedUser.toUserUpdatedEvent())
    }

    fun deleteUserImage(userId: UUID) {
        val user = findById(userId)
        user.imageUrl?.let { imageUrl ->
            imageStorageService.deleteImage(
                fileName = imageUrl.substringBefore("?"),
                folderName = profileImageDirectory
            )
            val savedUser = userRepository.save(user.copy(imageUrl = null))
            eventPublisher.publish(savedUser.toUserUpdatedEvent())
        }
    }
    fun findEmailsByUserIds(userIds: List<UUID>): Map<String, String> {
        val users = userRepository.findAllById(userIds)
        return users.associate { it.id.toString() to it.email }
    }
}