package org.spendoo.events.transactions

import org.spendoo.events.SpendooEvent
import java.util.UUID

class ExpenseSavedEvent(
    val userId: UUID,
    val categoryId: UUID
): SpendooEvent