package org.spendoo.chatbot.repository
import org.spendoo.chatbot.entity.UserChatbotUsage
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID
interface UserChatbotUsageRepository : JpaRepository<UserChatbotUsage, UUID>{
    fun findByUserId(userId: UUID): UserChatbotUsage?
}