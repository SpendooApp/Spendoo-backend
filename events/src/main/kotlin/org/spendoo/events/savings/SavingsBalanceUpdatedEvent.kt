package org.spendoo.events.savings

import org.spendoo.events.SpendooEvent
import java.util.UUID

data class SavingsBalanceUpdatedEvent(val userId: UUID, val isFirstDeposit: Boolean) : SpendooEvent
