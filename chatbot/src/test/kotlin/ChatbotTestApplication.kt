package org.spendoo.chatbot

import org.spendoo.chatbot.repository.AiChatMessageRepository
import org.spendoo.chatbot.repository.AiChatSessionRepository
import org.spendoo.chatbot.service.ChatbotService
import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.persistence.autoconfigure.EntityScan
import org.springframework.context.annotation.Bean
import org.springframework.data.jpa.repository.config.EnableJpaRepositories

@SpringBootConfiguration
@EnableAutoConfiguration
@EntityScan(basePackages = ["org.spendoo.chatbot.entity"])
@EnableJpaRepositories(basePackages = ["org.spendoo.chatbot.repository"])
class ChatbotTestApplication {

    @Bean
    fun chatbotService(
        chatSessionRepository: AiChatSessionRepository,
        chatMessageRepository: AiChatMessageRepository
    ): ChatbotService {
        return ChatbotService(
            chatSessionRepository = chatSessionRepository,
            chatMessageRepository = chatMessageRepository
        )
    }
}

