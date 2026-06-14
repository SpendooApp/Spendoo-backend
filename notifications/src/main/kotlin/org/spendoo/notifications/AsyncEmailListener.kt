package org.spendoo.notifications

import org.spendoo.client.ApiClient
import org.slf4j.LoggerFactory
import org.spendoo.events.notifications.EmailEvent
import org.springframework.context.event.EventListener
import org.springframework.http.HttpMethod
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component

@Component
class AsyncEmailListener(
    private val apiClient: ApiClient,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    private val emailServiceUrl = "https://spendoo-email-service.vercel.app/api/v1/send-email"

    @Async
    @EventListener
    fun handleEmailEvent(event: EmailEvent) {
        try {
            val requestBody = mapOf(
                "email" to event.to,
                "subject" to event.subject,
                "message" to event.text
            )
            apiClient.call(String::class.java){
                path = emailServiceUrl
                method = HttpMethod.POST
                body = requestBody
                addToken = false
            }

            log.info("Email sent successfully to ${event.to} with subject '${event.subject}'.")
        }
        catch (e: Exception) {
            log.error("Failed to send email to ${event.to}", e)
        }
    }
}