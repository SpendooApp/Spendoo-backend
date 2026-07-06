package org.spendoo.chatbot.service

import org.spendoo.chatbot.api.dto.request.ChatMessageRequestDto
import org.spendoo.chatbot.api.dto.response.AiChatResponse
import org.spendoo.chatbot.api.dto.response.ChatMessageResponseDto
import org.spendoo.chatbot.entity.AiChatMessage
import org.spendoo.chatbot.entity.AiChatSession
import org.spendoo.chatbot.entity.ChatSender
import org.spendoo.chatbot.entity.PlanCode
import org.spendoo.chatbot.entity.UserChatbotUsage
import org.spendoo.chatbot.repository.AiChatMessageRepository
import org.spendoo.chatbot.repository.AiChatSessionRepository
import org.spendoo.chatbot.repository.UserChatbotUsageRepository
import org.spendoo.client.ApiClient
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.time.LocalDate
import java.time.Instant
import java.util.UUID

@Service
class ChatbotService(
    private val chatSessionRepository: AiChatSessionRepository,
    private val chatMessageRepository: AiChatMessageRepository,
    private val userChatbotUsageRepository: UserChatbotUsageRepository,
    private val apiClient: ApiClient
) {

    @Transactional
    fun sendMessage(userId: UUID, message: ChatMessageRequestDto): ChatMessageResponseDto {
        validateChatUsage(userId)

        val session = chatSessionRepository.findByUserId(userId)
            ?: chatSessionRepository.save(AiChatSession(userId = userId, summary = ""))

        val userMessage = AiChatMessage(
            chatSession = session,
            sender = ChatSender.USER,
            content = message.content
        )
        chatMessageRepository.save(userMessage)

        val aiRequest = mapOf(
            "userId" to userId.toString(),
            "message" to message.content,
            "chatSummary" to session.summary
        )

        val aiResponse = apiClient.call(AiChatResponse::class.java) {
            callAIService = true
            path = "/api/v1/chatbot/"
            method = HttpMethod.POST
            body = aiRequest
        } ?: throw IllegalStateException("Failed to get response from AI chatbot")

        val botReply = aiResponse.response
        val updatedSummary = aiResponse.chatSummary

        val botMessage = AiChatMessage(
            chatSession = session,
            sender = ChatSender.BOT,
            content = botReply
        )
        val savedBotMessage = chatMessageRepository.save(botMessage)

        val updatedSession = session.copy(
            summary = updatedSummary,
            updatedAt = Instant.now()
        )
        chatSessionRepository.save(updatedSession)

        incrementChatUsage(userId)
        return ChatMessageResponseDto(
            id = savedBotMessage.id,
            sender = savedBotMessage.sender,
            content = savedBotMessage.content,
            timestamp = savedBotMessage.timestamp
        )
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
            updatedAt = Instant.now()
        )
        chatSessionRepository.save(resetSession)
    }

    private fun getCurrentPlanCode(userId: UUID): PlanCode {
        val response = apiClient.call(PlanCode::class.java) {
            path = "/api/v1/subscriptions/current"
            method = HttpMethod.GET
            addToken = true
            this.userId = userId
        }
        return response ?: PlanCode.FREE
    }

    private fun validateChatUsage(userId: UUID) {
        val currentPlan = getCurrentPlanCode(userId)
        var usage = userChatbotUsageRepository.findByUserId(userId) ?: UserChatbotUsage(userId = userId)

        if (LocalDate.now().isAfter(usage.resetDate)) {
            usage = usage.copy(messageCount = 0, resetDate = LocalDate.now())
            userChatbotUsageRepository.save(usage)
        }

        val limit = when (currentPlan) {
            PlanCode.FREE -> 15
            PlanCode.BASIC -> 50
            PlanCode.PRO -> 150
        }

        if (usage.messageCount >= limit) {
            throw ResponseStatusException(HttpStatus.PAYMENT_REQUIRED)
        }
    }

    private fun incrementChatUsage(userId: UUID) {

        val usage = userChatbotUsageRepository.findByUserId(userId) ?: UserChatbotUsage(userId = userId)
        val updatedUsage = usage.copy(messageCount = usage.messageCount + 1)
        userChatbotUsageRepository.save(updatedUsage)
    }
}