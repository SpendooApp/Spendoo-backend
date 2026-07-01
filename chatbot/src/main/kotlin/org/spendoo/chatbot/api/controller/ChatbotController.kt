package org.spendoo.chatbot.api.controller

import jakarta.validation.Valid
import org.spendoo.chatbot.api.dto.request.ChatMessageRequestDto
import org.spendoo.chatbot.api.dto.response.ChatMessageResponseDto
import org.spendoo.chatbot.service.ChatbotService
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1/chatbot")
class ChatbotController(
    private val chatbotService: ChatbotService
) {

    @PostMapping("/send")
    fun sendMessage(
        @AuthenticationPrincipal userId: UUID,
        @Valid @RequestBody request: ChatMessageRequestDto
    ): ResponseEntity<ChatMessageResponseDto> {
        val botResponse = chatbotService.sendMessage(userId, request)
        return ResponseEntity.ok(botResponse)
    }

    @GetMapping("/history")
    fun getChatHistory(
        @AuthenticationPrincipal userId: UUID,
        pageable: Pageable
    ): ResponseEntity<Page<ChatMessageResponseDto>> {
        val history = chatbotService.getChatHistory(userId, pageable)
        return ResponseEntity.ok(history)
    }

    @DeleteMapping("/clear")
    fun clearChat(
        @AuthenticationPrincipal userId: UUID,
    ): ResponseEntity<Unit> {
        chatbotService.clearChat(userId)
        return ResponseEntity.ok().build()
    }

}