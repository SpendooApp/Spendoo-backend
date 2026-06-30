package org.spendoo.chatbot.service

import org.spendoo.chatbot.api.dto.request.ChatMessageRequestDto
import org.spendoo.chatbot.api.dto.response.ChatMessageResponseDto
import org.spendoo.chatbot.entity.AiChatMessage
import org.spendoo.chatbot.entity.AiChatSession
import org.spendoo.chatbot.entity.ChatSender
import org.spendoo.chatbot.repository.AiChatMessageRepository
import org.spendoo.chatbot.repository.AiChatSessionRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.UUID

@Service
class ChatbotService(
    private val chatSessionRepository: AiChatSessionRepository,
    private val chatMessageRepository: AiChatMessageRepository
) {

    @Transactional
    fun sendMessage(userId: UUID, message: ChatMessageRequestDto) {

        val session = chatSessionRepository.findByUserId(userId)
            ?: chatSessionRepository.save(AiChatSession(userId = userId, summary = ""))

        val userMessage = AiChatMessage(
            chatSession = session,
            sender = ChatSender.USER,
            content = message.content
        )
        chatMessageRepository.save(userMessage)

        val botReply = "ex for bot message!"
        val updatedSummary = "Updated summary after user said: ${message.content}"

        val botMessage = AiChatMessage(
            chatSession = session,
            sender = ChatSender.BOT,
            content = botReply
        )
        chatMessageRepository.save(botMessage)

        val updatedSession = session.copy(
            summary = updatedSummary,
            updatedAt = LocalDateTime.now()
        )
        chatSessionRepository.save(updatedSession)
    }

    @Transactional(readOnly = true)
    fun getChatHistory(userId: UUID, pageable: Pageable): Page<ChatMessageResponseDto> {
        val session = chatSessionRepository.findByUserId(userId) ?: return Page.empty(pageable)

        val messagesPage =  chatMessageRepository.findByChatSessionIdOrderByTimestampAsc(session.id, pageable)

        return messagesPage.map { entity ->
            ChatMessageResponseDto(
                id = entity.id,
                sender = entity.sender,
                content = entity.content,
                timestamp = entity.timestamp
            )
        }
    }

    @Transactional
    fun clearChat(userId: UUID) {
        val session = chatSessionRepository.findByUserId(userId) ?: return

        chatMessageRepository.deleteAllByChatSessionId(session.id)

        val resetSession = session.copy(
            summary = "",
            updatedAt = LocalDateTime.now()
        )
        chatSessionRepository.save(resetSession)
    }
}