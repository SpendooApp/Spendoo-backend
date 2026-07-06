package org.spendoo.chatbot.integration

import com.google.common.truth.Truth.assertThat
import com.ninjasquad.springmockk.MockkBean
import io.mockk.clearMocks
import io.mockk.every
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.spendoo.chatbot.ChatbotTestApplication
import org.spendoo.chatbot.api.dto.request.ChatMessageRequestDto
import org.spendoo.chatbot.api.dto.response.AiChatResponse
import org.spendoo.chatbot.entity.AiChatMessage
import org.spendoo.chatbot.entity.AiChatSession
import org.spendoo.chatbot.entity.ChatSender
import org.spendoo.chatbot.entity.PlanCode
import org.spendoo.chatbot.repository.AiChatMessageRepository
import org.spendoo.chatbot.repository.AiChatSessionRepository
import org.spendoo.chatbot.repository.UserChatbotUsageRepository
import org.spendoo.chatbot.service.ChatbotService
import org.spendoo.client.ApiClient
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.domain.PageRequest
import org.springframework.test.context.ActiveProfiles
import java.time.Instant
import java.util.UUID

@SpringBootTest(classes = [ChatbotTestApplication::class])
@ActiveProfiles("test")
class ChatbotServiceIntegrationTest {

    @Autowired
    private lateinit var chatbotService: ChatbotService

    @Autowired
    private lateinit var chatSessionRepository: AiChatSessionRepository

    @Autowired
    private lateinit var chatMessageRepository: AiChatMessageRepository

    @Autowired
    private lateinit var userChatbotUsageRepository: UserChatbotUsageRepository

    @MockkBean(relaxed = true)
    private lateinit var apiClient: ApiClient

    @BeforeEach
    fun setUp() {
        chatMessageRepository.deleteAll()
        chatSessionRepository.deleteAll()
        userChatbotUsageRepository.deleteAll()
        clearMocks(apiClient)
        every { apiClient.call(any<Class<PlanCode>>(), any()) } returns PlanCode.FREE

        every { apiClient.call(eq(AiChatResponse::class.java), any()) } returns AiChatResponse(
            response = "This is a mocked bot reply",
            chatSummary = "how to save money? user asked about transportation."
        )

    }

    @Test
    fun `sendMessage creates new session and saves messages if session does not exist`() {
        val userId = UUID.randomUUID()
        val request = ChatMessageRequestDto(content = "how to save money?")

        val response = chatbotService.sendMessage(userId, request)

        assertThat(response).isNotNull()
        assertThat(response.content).isNotNull()

        val session = chatSessionRepository.findByUserId(userId)
        assertThat(session).isNotNull()
        assertThat(session!!.summary).contains("how to save money?")

        val messages = chatMessageRepository.findAll()
        assertThat(messages).hasSize(2)
        assertThat(messages.any { it.sender == ChatSender.USER }).isTrue()
        assertThat(messages.any { it.sender == ChatSender.BOT }).isTrue()
    }

    @Test
    fun `sendMessage uses existing session and appends messages`() {
        val userId = UUID.randomUUID()
        val existingSession = chatSessionRepository.save(
            AiChatSession(
                userId = userId,
                summary = "Old summary",
                createdAt = Instant.now(),
                updatedAt = Instant.now()
            )
        )

        val request = ChatMessageRequestDto(content = "should I start saving by using public transportation instead of driving?")
        chatbotService.sendMessage(userId, request)

        val sessions = chatSessionRepository.findAll()
        assertThat(sessions).hasSize(1)
        assertThat(sessions.first().id).isEqualTo(existingSession.id)

        val messages = chatMessageRepository.findAll()
        assertThat(messages).hasSize(2)
    }

    @Test
    fun `getChatHistory returns paginated messages for user ordered by time`() {
        val userId = UUID.randomUUID()
        val session = chatSessionRepository.save(
            AiChatSession(
                userId = userId,
                summary = "History test",
                createdAt = Instant.now(),
                updatedAt = Instant.now()
            )
        )

        chatMessageRepository.save(
            AiChatMessage(
                chatSession = session,
                sender = ChatSender.USER,
                content = "User msg",
                timestamp = Instant.now().atZone(java.time.ZoneOffset.UTC).minusMinutes(2).toInstant()
            )
        )
        chatMessageRepository.save(
            AiChatMessage(chatSession = session, sender = ChatSender.BOT, content = "Bot reply", timestamp = Instant.now())
        )

        val pageable = PageRequest.of(0, 10)
        val historyPage = chatbotService.getChatHistory(userId, pageable)

        assertThat(historyPage.content).hasSize(2)
        assertThat(historyPage.content[0].sender).isEqualTo(ChatSender.USER)
        assertThat(historyPage.content[1].sender).isEqualTo(ChatSender.BOT)
    }

    @Test
    fun `getChatHistory returns empty page if session does not exist`() {
        val randomUserId = UUID.randomUUID()
        val pageable = PageRequest.of(0, 10)

        val historyPage = chatbotService.getChatHistory(randomUserId, pageable)

        assertThat(historyPage.isEmpty).isTrue()
    }

    @Test
    fun `clearChat deletes all messages and resets session summary`() {
        val userId = UUID.randomUUID()
        val session = chatSessionRepository.save(
            AiChatSession(
                userId = userId,
                summary = "Some summary that should be cleared",
                createdAt = Instant.now(),
                updatedAt = Instant.now()
            )
        )
        chatMessageRepository.save(
            AiChatMessage(chatSession = session, sender = ChatSender.USER, content = "User msg")
        )

        chatbotService.clearChat(userId)

        val messages = chatMessageRepository.findAll()
        assertThat(messages).isEmpty()

        val updatedSession = chatSessionRepository.findByUserId(userId)
        assertThat(updatedSession).isNotNull()
        assertThat(updatedSession!!.summary).isEmpty()
    }
}