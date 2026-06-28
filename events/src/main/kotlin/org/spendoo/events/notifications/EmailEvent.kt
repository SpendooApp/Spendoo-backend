package org.spendoo.events.notifications

import org.spendoo.events.SpendooEvent

data class EmailEvent(
    val to: String,
    val subject: String,
    val text: String,
) : SpendooEvent