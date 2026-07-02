package org.spendoo.identity.api.dto.request

import org.spendoo.identity.entity.BillingCycle
import java.util.UUID

data class SubscribePlanRequest(

    var planId: UUID,

    var billingCycle: BillingCycle?
)
