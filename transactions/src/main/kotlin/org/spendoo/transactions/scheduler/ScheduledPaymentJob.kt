package org.spendoo.transactions.scheduler

import org.spendoo.events.notifications.EmailEvent
import org.spendoo.events.publisher.SpendooEventPublisher
import org.spendoo.identity.repository.UserRepository
import org.spendoo.transactions.repository.ScheduledPaymentRepository
import org.spendoo.transactions.service.ScheduledPaymentService
import org.springframework.data.domain.PageRequest
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Component
class ScheduledPaymentJob (
    private val scheduledPaymentRepository: ScheduledPaymentRepository,
    private val scheduledPaymentService: ScheduledPaymentService,
    private val publisher: SpendooEventPublisher,
    private val userRepository: UserRepository
){

    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    fun processScheduledTasks(){
        val now = LocalDateTime.now()
        processReminders(now)
        processAutoPayments(now)
    }

    fun processReminders(now: LocalDateTime) {
        val batchRequest = PageRequest.of(0, BATCH_SIZE)
        var hasRecords = true

        while (hasRecords) {
            val paymentsToNotify = scheduledPaymentRepository.findByNextReminderDateBeforeAndIsNotifiedFalse(now, batchRequest)

            if (paymentsToNotify.isEmpty){
                hasRecords = false
                continue
            }

            for(payment in paymentsToNotify.content) {
                val user = userRepository.findById(payment.userId).orElse(null) ?: continue

                val reminderText = """
                    Hello,
                    
                    This is a gentle reminder that your payment for '${payment.title}' 
                    amounting to ${payment.amount} is due on ${payment.nextDueDate}.
                    
                    Thanks,
                    Spendoo Team
                """.trimIndent()

                val event = EmailEvent(
                    to = user.email,
                    subject = "Spendoo - Upcoming Payment Reminder",
                    text = reminderText,
                )
                publisher.publish(event)

                val updatedPayment = payment.copy(isNotified = true)
                scheduledPaymentRepository.save(updatedPayment)

            }
        }
    }

    private fun processAutoPayments(now: LocalDateTime) {
        val batchRequest = PageRequest.of(0, BATCH_SIZE)
        var hasRecords = true

        while (hasRecords) {
            val paymentsToNotify = scheduledPaymentRepository.findByNextDueDateBefore(now, batchRequest)

            if (paymentsToNotify.isEmpty){
                hasRecords = false
                continue
            }

            for(payment in paymentsToNotify.content) {
                try{
                    scheduledPaymentService.payScheduledItem(payment.userId, payment.id)
                } catch (ex: Exception) {
                    println("Error processing auto-payment for paymentId: ${payment.id}. Reason: ${ex.message}")
                }
            }
        }
    }

    companion object {
        private const val BATCH_SIZE = 50
    }

}