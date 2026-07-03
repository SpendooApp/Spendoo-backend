package org.spendoo.transactions.repository

import org.spendoo.transactions.entity.ProposedAction
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface ProposedActionRepository : JpaRepository<ProposedAction, UUID> {
}