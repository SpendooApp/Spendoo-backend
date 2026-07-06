package org.spendoo.chatbot.entity

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "ai_chat_messages", schema = "chatbot")
data class AiChatMessage(
    @Id
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    val id: UUID = UUID.randomUUID(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    val chatSession: AiChatSession,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val sender: ChatSender,

    @Column(columnDefinition = "TEXT", nullable = false)
    val content: String,

    @Column(nullable = false)
    val timestamp: Instant = Instant.now()
)