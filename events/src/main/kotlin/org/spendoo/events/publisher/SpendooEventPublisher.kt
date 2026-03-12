package org.spendoo.events.publisher

import org.spendoo.events.SpendooEvent

interface SpendooEventPublisher {
    fun publish(event: SpendooEvent)
}