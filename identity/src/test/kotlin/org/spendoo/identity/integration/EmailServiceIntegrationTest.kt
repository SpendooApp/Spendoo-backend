package org.spendoo.identity.integration

import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.spendoo.events.notifications.EmailEvent
import org.spendoo.events.publisher.SpendooEventPublisher
import org.spendoo.identity.service.EmailService

class EmailServiceIntegrationTest {

    private val eventPublisher = mockk<SpendooEventPublisher>(relaxed = true)
    private val emailService = EmailService(eventPublisher)

    @Test
    fun `sendOtp publishes password reset email event`() {
        val eventSlot = slot<EmailEvent>()
        every { eventPublisher.publish(capture(eventSlot)) } returns Unit

        emailService.sendOtp("user@example.com", "54321")

        verify(exactly = 1) { eventPublisher.publish(any()) }
        assertThat(eventSlot.captured.to).isEqualTo("user@example.com")
        assertThat(eventSlot.captured.subject).isEqualTo("Spendoo - Password Reset Code")
        assertThat(eventSlot.captured.text).contains("54321")
        assertThat(eventSlot.captured.text).contains("reset your password")
    }

    @Test
    fun `sendWelcomeVerificationOtp publishes welcome email event`() {
        val eventSlot = slot<EmailEvent>()
        every { eventPublisher.publish(capture(eventSlot)) } returns Unit

        emailService.sendWelcomeVerificationOtp("new-user@example.com", "12345")

        verify(exactly = 1) { eventPublisher.publish(any()) }
        assertThat(eventSlot.captured.to).isEqualTo("new-user@example.com")
        assertThat(eventSlot.captured.subject).isEqualTo("Spendoo - Welcome! Verify your email")
        assertThat(eventSlot.captured.text).contains("12345")
        assertThat(eventSlot.captured.text).contains("Welcome to Spendoo")
    }

    @Test
    fun `generateOtp returns a 5-digit numeric string`() {
        val otp = emailService.generateOtp()

        assertThat(otp).matches("\\d{5}")
    }
}

