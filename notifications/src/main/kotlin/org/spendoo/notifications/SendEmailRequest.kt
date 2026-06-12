package org.spendoo.notifications

data class SendEmailRequest(
    val email: String,
    val subject: String,
    val message: String
)