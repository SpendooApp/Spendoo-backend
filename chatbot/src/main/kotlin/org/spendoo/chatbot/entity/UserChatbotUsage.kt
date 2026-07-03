package org.spendoo.chatbot.entity

import jakarta.persistence.*
import java.time.LocalDate
import java.util.UUID

@Entity
@Table(name = "user_chatbot_usage", schema = "chatbot")
data class UserChatbotUsage(
    @Id
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    val id: UUID = UUID.randomUUID(),

    @Column(nullable = false, unique = true)
    val userId: UUID,

    @Column(nullable = false)
    val messageCount: Int = 0,

    @Column(nullable = false)
    val resetDate: LocalDate = LocalDate.now()
)
