package org.spendoo.transactions.service

import org.spendoo.transactions.api.dto.request.CreateTransactionRequest
import org.spendoo.transactions.mapper.toEntity
import org.spendoo.transactions.repository.CategoryRepository
import org.spendoo.transactions.repository.TransactionRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class TransactionService(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository
) {

    @Transactional
    fun createTransactions(userId: UUID, request: CreateTransactionRequest) {

        val transactionsToSave = request.entries.map { entry ->
            val category = categoryRepository.findById(entry.categoryId)
                .orElseThrow {
                    IllegalArgumentException("Category not found with ID: ${entry.categoryId}")
                }

            entry.toEntity(userId, request, category)
        }

        transactionRepository.saveAll(transactionsToSave)
    }
}