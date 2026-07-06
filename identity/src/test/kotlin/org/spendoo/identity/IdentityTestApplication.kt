package org.spendoo.identity

import io.mockk.mockk
import org.spendoo.events.publisher.SpendooEventPublisher
import org.spendoo.identity.repository.EmailVerificationRepository
import org.spendoo.identity.repository.FollowCodeRepository
import org.spendoo.identity.repository.FollowRepository
import org.spendoo.identity.repository.RefreshTokenRepository
import org.spendoo.identity.repository.SubscriptionPlanRepository
import org.spendoo.identity.repository.UserRepository
import org.spendoo.identity.repository.UserSubscriptionRepository
import org.spendoo.identity.security.JwtUtil
import org.spendoo.identity.service.AuthService
import org.spendoo.identity.service.EmailService
import org.spendoo.identity.service.FollowCodeService
import org.spendoo.identity.service.FollowService
import org.spendoo.identity.service.SubscriptionService
import org.spendoo.identity.service.UserService
import org.spendoo.storage.service.ImageStorageService
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.persistence.autoconfigure.EntityScan
import org.springframework.context.annotation.Bean
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder

@SpringBootConfiguration
@EnableAutoConfiguration
@EntityScan(basePackages = ["org.spendoo.identity.entity"])
@EnableJpaRepositories(basePackages = ["org.spendoo.identity.repository"])
class IdentityTestApplication {

    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()

    @Bean
    fun emailService(): EmailService = mockk(relaxed = true)

    @Bean
    fun jwtUtil(): JwtUtil = mockk(relaxed = true)

    @Bean
    fun spendooEventPublisher(): SpendooEventPublisher = mockk(relaxed = true)

    @Bean
    fun imageStorageService(): ImageStorageService = mockk(relaxed = true)

    @Bean
    fun authService(
        userRepository: UserRepository,
        refreshTokenRepository: RefreshTokenRepository,
        emailVerificationRepository: EmailVerificationRepository,
        subscriptionPlanRepository: SubscriptionPlanRepository,
        userSubscriptionRepository: UserSubscriptionRepository,
        emailService: EmailService,
        passwordEncoder: PasswordEncoder,
        jwtUtil: JwtUtil,
        spendooEventPublisher: SpendooEventPublisher
    ): AuthService {
        return AuthService(
            userRepository = userRepository,
            refreshTokenRepository = refreshTokenRepository,
            otpRepository = emailVerificationRepository,
            emailService = emailService,
            passwordEncoder = passwordEncoder,
            jwtUtil = jwtUtil,
            spendooEventPublisher = spendooEventPublisher,
            subscriptionPlanRepository = subscriptionPlanRepository,
            userSubscriptionRepository = userSubscriptionRepository,
        )
    }

    @Bean
    fun userService(
        userRepository: UserRepository,
        userSubscriptionRepository: UserSubscriptionRepository,
        imageStorageService: ImageStorageService,
        spendooEventPublisher: SpendooEventPublisher,
        followCodeRepository: FollowCodeRepository,
        followCodeService: FollowCodeService,
        @Value("\${identity.resources.profile-image-directory}") profileImageDirectory: String
    ): UserService {
        return UserService(
            userRepository = userRepository,
            userSubscriptionRepository = userSubscriptionRepository,
            imageStorageService = imageStorageService,
            eventPublisher = spendooEventPublisher,
            profileImageDirectory = profileImageDirectory,
            followCodeRepository = followCodeRepository,
            followCodeService = followCodeService

        )
    }

    @Bean
    fun followCodeService(
        userRepository: UserRepository,
        followCodeRepository: FollowCodeRepository
    ): FollowCodeService {
        return FollowCodeService(
            userRepository = userRepository,
            followCodeRepository = followCodeRepository
        )
    }


    @Bean
    fun followService(
        userRepository: UserRepository,
        followCodeRepository: FollowCodeRepository,
        followRepository: FollowRepository,
        spendooEventPublisher: SpendooEventPublisher
    ): FollowService {
        return FollowService(
            userRepository = userRepository,
            followCodeRepository = followCodeRepository,
            followRepository = followRepository,
            eventPublisher = spendooEventPublisher
        )
    }

    @Bean
    fun subscriptionService(
        subscriptionPlanRepository: SubscriptionPlanRepository,
        userSubscriptionRepository: UserSubscriptionRepository
    ): SubscriptionService {
        return SubscriptionService(
            subscriptionPlanRepository = subscriptionPlanRepository,
            userSubscriptionRepository = userSubscriptionRepository
        )
    }

}

