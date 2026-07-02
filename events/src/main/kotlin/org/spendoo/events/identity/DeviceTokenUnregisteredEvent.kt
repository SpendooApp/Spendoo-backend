package org.spendoo.events.identity

import org.spendoo.events.SpendooEvent

data class DeviceTokenUnregisteredEvent(
    val token: String
) : SpendooEvent
