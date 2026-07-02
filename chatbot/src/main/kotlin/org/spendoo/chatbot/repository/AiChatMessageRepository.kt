package org.spendoo.chatbot.repository

import org.spendoo.chatbot.entity.AiChatMessage
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface AiChatMessageRepository : JpaRepository<AiChatMessage, UUID> {
    fun findByChatSessionIdOrderByTimestampAsc(sessionId: UUID, pageable: Pageable): Page<AiChatMessage>
    fun deleteAllByChatSessionId(sessionId: UUID)
}