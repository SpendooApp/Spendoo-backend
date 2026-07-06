package org.spendoo.chatbot.api.dto.response

import org.spendoo.chatbot.entity.ChatSender
import java.time.Instant
import java.util.UUID

data class ChatMessageResponseDto(
    val id: UUID,
    val sender: ChatSender,
    val content: String,
    val timestamp: Instant
)