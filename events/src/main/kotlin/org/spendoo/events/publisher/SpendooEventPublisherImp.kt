package org.spendoo.events.publisher

import org.spendoo.events.SpendooEvent
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component

@Component
internal class SpendooEventPublisherImp(
    private val applicationEventPublisher: ApplicationEventPublisher
) : SpendooEventPublisher {

    override fun publish(event: SpendooEvent) {
        applicationEventPublisher.publishEvent(event)
    }
}