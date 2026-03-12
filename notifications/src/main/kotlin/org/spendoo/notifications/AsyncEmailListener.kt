package org.spendoo.notifications

import org.slf4j.LoggerFactory
import org.spendoo.events.notifications.EmailEvent
import org.springframework.context.event.EventListener
import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component

@Component
class AsyncEmailListener(
    private val mailSender: JavaMailSender,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Async
    @EventListener
    fun handleEmailEvent(event: EmailEvent) {
        try {
            val message = SimpleMailMessage()
            message.from = "noreply@spendoo.com"
            message.setTo(event.to)
            message.subject = event.subject
            message.text = event.text
            mailSender.send(message)
            log.info("Sent email to ${event.to} with subject='${event.subject}'")
        } catch (ex: Exception) {
            log.error("Failed to send email to ${event.to}", ex)
        }
    }
}