package org.spendoo.notifications

import org.slf4j.LoggerFactory
import org.spendoo.events.notifications.EmailEvent
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate

@Component
class AsyncEmailListener(
    private val restTemplate: RestTemplate,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    private val emailServiceUrl = "https://spendoo-email-service.vercel.app/api/v1/send-email"

    @Async
    @EventListener
    fun handleEmailEvent(event: EmailEvent) {
        try {
            val request = SendEmailRequest(
                email = event.to,
                subject = event.subject,
                message = event.text
            )
            val response = restTemplate.postForEntity(emailServiceUrl, request, String::class.java)

            log.info("Email sent to ${event.to} with subject '${event.subject}'. Response: ${response.statusCode}")
        }
        catch (e: Exception) {
            log.error("Failed to send email to ${event.to}", e)
        }
    }
}