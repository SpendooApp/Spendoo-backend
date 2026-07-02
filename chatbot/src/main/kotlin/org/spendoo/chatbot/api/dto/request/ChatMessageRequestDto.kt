package org.spendoo.chatbot.api.dto.request

import jakarta.validation.constraints.NotBlank

data class ChatMessageRequestDto(
    @field:NotBlank(message = "Message content cannot be empty")
    val content: String
)