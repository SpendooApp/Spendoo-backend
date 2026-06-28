package org.spendoo.events.savings

import org.spendoo.events.SpendooEvent
import java.util.UUID

data class SavingGoalCompletedEvent(val userId: UUID, val goalPriority: Int) : SpendooEvent
