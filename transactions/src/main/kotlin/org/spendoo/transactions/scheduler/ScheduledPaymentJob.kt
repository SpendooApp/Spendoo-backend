package org.spendoo.transactions.scheduler

import org.slf4j.LoggerFactory
import org.spendoo.events.notifications.UserNotificationsEvent
import org.spendoo.events.publisher.SpendooEventPublisher
import org.spendoo.events.notifications.NotificationDetails
import org.spendoo.events.notifications.utils.NotificationType
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
    private val publisher: SpendooEventPublisher
){

    private val log = LoggerFactory.getLogger(ScheduledPaymentJob::class.java)


    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    fun processScheduledTasks(){
        val now = LocalDateTime.now()
        processReminders(now)
        processAutoPayments(now)
    }

    private fun processReminders(now: LocalDateTime) {
        val batchRequest = PageRequest.of(0, BATCH_SIZE)

        while (true) {
            val paymentsToNotify = scheduledPaymentRepository.findByNextReminderDateBeforeAndIsNotifiedFalse(now, batchRequest)

            if (paymentsToNotify.isEmpty){
                break
            }

            val detailsList = paymentsToNotify.content.map{ payment ->
                NotificationDetails(
                    userId = payment.userId,
                    subject = "Spendoo - Upcoming Payment Reminder",
                    message = """
                        Hello,
                        
                        This is a gentle reminder that your payment for '${payment.title}' 
                        amounting to ${payment.amount} is due on ${payment.nextDueDate}.
                        
                        Thanks,
                        Spendoo Team
                    """.trimIndent(),
                    type = NotificationType.PAYMENT_REMINDER
                )
            }
            publisher.publish(UserNotificationsEvent(notifications = detailsList))

            val paymentIds = paymentsToNotify.content.map { it.id }
            scheduledPaymentRepository.markPaymentsNotified(paymentIds)

            if (paymentsToNotify.isLast) {
                break
            }
        }
    }


    private fun processAutoPayments(now: LocalDateTime) {
        val batchRequest = PageRequest.of(0, BATCH_SIZE)

        while (true) {
            val paymentsToNotify = scheduledPaymentRepository.findByNextDueDateBefore(now, batchRequest)

            if (paymentsToNotify.isEmpty){
                break
            }

            var processedAnySuccess = false

            for(payment in paymentsToNotify.content) {
                try{
                    scheduledPaymentService.payScheduledItem(payment.userId, payment.id)
                    processedAnySuccess = true
                } catch (ex: Exception) {
                    log.error("Error processing auto-payment for paymentId: ${payment.id}. Reason: ${ex.message}")
                }
            }

            if (!processedAnySuccess) {
                log.warn("Breaking auto-payment loop to prevent infinite retry loop on failing payments.")
                break
            }
            if (paymentsToNotify.isLast) {
                break
            }
        }
    }

    companion object {
        private const val BATCH_SIZE = 50
    }

}