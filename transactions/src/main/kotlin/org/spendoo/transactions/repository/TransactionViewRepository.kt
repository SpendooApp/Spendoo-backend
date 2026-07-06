package org.spendoo.transactions.repository

import org.spendoo.transactions.entity.TransactionView
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import java.time.Instant
import java.util.*

interface TransactionViewRepository : JpaRepository<TransactionView, UUID>, JpaSpecificationExecutor<TransactionView> {
    
    fun findAllByUserIdAndTransactionDateBetween(
        userId: UUID,
        startDate: Instant,
        endDate: Instant,
        pageable: Pageable
    ): Page<TransactionView>

}