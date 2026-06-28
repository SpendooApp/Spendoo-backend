package org.spendoo.events.transactions

import org.spendoo.events.SpendooEvent
import java.util.UUID

data class TransactionCreatedEvent(val userId: UUID, val distinctCategoryCount: Int) : SpendooEvent
