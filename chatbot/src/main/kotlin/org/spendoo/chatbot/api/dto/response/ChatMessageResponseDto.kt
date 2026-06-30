package org.spendoo.chatbot.api.dto.response

import org.spendoo.chatbot.entity.ChatSender
import java.time.LocalDateTime
import java.util.UUID

data class ChatMessageResponseDto(
    val id: UUID,
    val sender: ChatSender,
    val content: String,
    val timestamp: LocalDateTime
)