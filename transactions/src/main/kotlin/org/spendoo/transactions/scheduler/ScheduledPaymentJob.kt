package org.spendoo.transactions.scheduler

import org.spendoo.events.notifications.EmailEvent
import org.spendoo.events.publisher.SpendooEventPublisher
import org.spendoo.identity.repository.UserRepository
import org.spendoo.transactions.repository.ScheduledPaymentRepository
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Component
class ScheduledPaymentJob (
    private val scheduledPaymentRepository: ScheduledPaymentRepository,
    private val publisher: SpendooEventPublisher,
    private val userRepository: UserRepository
){

    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    fun sendPaymentReminders() {
        val now = LocalDateTime.now()
        val paymentsToNotify = scheduledPaymentRepository.findByNextReminderDateBeforeAndIsNotifiedFalse(now)

        for (payment in paymentsToNotify) {
            val userEmail = userRepository.findById(payment.userId)
                .orElseThrow { RuntimeException("User not found for payment: ${payment.id}") }
                .email
            val reminderText = """
                Hello,
                
                This is a gentle reminder that your payment for '${payment.title}' 
                amounting to ${payment.amount} is due on ${payment.nextDueDate}.
                
                Thanks,
                Spendoo Team
            """.trimIndent()
            
            val event = EmailEvent(
                to = userEmail,
                subject = "Spendoo - Upcoming Payment Reminder",
                text = reminderText,
            )

            publisher.publish(event)

            val updatedPayment = payment.copy(isNotified = true)
            scheduledPaymentRepository.save(updatedPayment)
        }
    }


}