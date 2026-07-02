package org.spendoo.chatbot.repository


import org.spendoo.chatbot.entity.AiChatSession
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID


interface AiChatSessionRepository : JpaRepository<AiChatSession, UUID> {
    fun findByUserId(userId: UUID): AiChatSession?
}