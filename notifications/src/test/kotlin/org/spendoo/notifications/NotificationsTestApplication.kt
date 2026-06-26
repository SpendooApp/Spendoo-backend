package org.spendoo.notifications

import org.spendoo.notifications.repository.NotificationRepository
import org.spendoo.notifications.service.NotificationService
import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.persistence.autoconfigure.EntityScan
import org.springframework.context.annotation.Bean
import org.springframework.data.jpa.repository.config.EnableJpaRepositories

@SpringBootConfiguration
@EnableAutoConfiguration
@EntityScan(basePackages = ["org.spendoo.notifications.entity"])
@EnableJpaRepositories(basePackages = ["org.spendoo.notifications.repository"])
class NotificationsTestApplication {

    @Bean
    fun notificationService(
        notificationRepository: NotificationRepository
    ): NotificationService {
        return NotificationService(
            notificationRepository = notificationRepository
        )
    }

}