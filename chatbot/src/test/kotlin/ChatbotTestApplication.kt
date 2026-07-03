package org.spendoo.chatbot

import io.mockk.mockk
import org.spendoo.chatbot.repository.AiChatMessageRepository
import org.spendoo.chatbot.repository.AiChatSessionRepository
import org.spendoo.chatbot.repository.UserChatbotUsageRepository
import org.spendoo.chatbot.service.ChatbotService
import org.spendoo.client.ApiClient
import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.persistence.autoconfigure.EntityScan
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.data.jpa.repository.config.EnableJpaRepositories

@SpringBootConfiguration
@EnableAutoConfiguration
@EntityScan(basePackages = ["org.spendoo.chatbot.entity"])
@EnableJpaRepositories(basePackages = ["org.spendoo.chatbot.repository"])
class ChatbotTestApplication {

    @Bean
    fun apiClient(): ApiClient {
        return mockk<ApiClient>(relaxed = true)
    }
    @Bean
    fun chatbotService(
        chatSessionRepository: AiChatSessionRepository,
        chatMessageRepository: AiChatMessageRepository,
        userChatbotUsageRepository: UserChatbotUsageRepository,
        apiClient: ApiClient
    ): ChatbotService {
        return ChatbotService(
            chatSessionRepository = chatSessionRepository,
            chatMessageRepository = chatMessageRepository,
            userChatbotUsageRepository = userChatbotUsageRepository,
            apiClient = apiClient
        )
    }
}

