package org.spendoo.transactions.service

import org.spendoo.transactions.api.dto.request.CreateTransactionRequest
import org.spendoo.transactions.api.dto.request.TransactionUpdateRequest
import org.spendoo.transactions.api.dto.response.TransactionResponse
import org.spendoo.transactions.api.dto.response.toResponse
import org.spendoo.transactions.entity.Transaction
import org.spendoo.transactions.mapper.toEntity
import org.spendoo.transactions.repository.CategoryRepository
import org.spendoo.transactions.repository.TransactionRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.*

@Service
class TransactionService(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository
) {

    @Transactional
    fun createTransactions(userId: UUID, request: CreateTransactionRequest) {

        val transactionsToSave = request.entries.map { entry ->
            val category =
                categoryRepository.findByIdAndUserIdAndIsDeletedFalse(entry.categoryId, userId)
                    ?: throw IllegalArgumentException("Category not found with ID: ${entry.categoryId}")
            entry.toEntity(userId, request, category)
        }

        transactionRepository.saveAll(transactionsToSave)
    }

    @Transactional
    fun updateTransaction(transactionId: UUID, userId: UUID, updateRequest: TransactionUpdateRequest): Transaction {
        val transaction = transactionRepository.findByIdAndUserId(transactionId, userId)
            ?: throw IllegalArgumentException("Transaction not found")

        val category = updateRequest.categoryId?.let { categoryId ->
            categoryRepository.findByIdAndUserIdAndIsDeletedFalse(categoryId, userId)
                ?: throw IllegalArgumentException("Category not found with ID: $categoryId")
        } ?: transaction.category

        val updatedTransaction = transaction.copy(
            title = updateRequest.title,
            transactionDate = updateRequest.transactionDate ?: transaction.transactionDate,
            note = updateRequest.note ?: transaction.note,
            amount = updateRequest.amount ?: transaction.amount,
            category = category
        )

        return transactionRepository.save(updatedTransaction)
    }

    @Transactional
    fun getTransactionById(transactionId: UUID, userId: UUID): Transaction {
        return transactionRepository.findByIdAndUserId(transactionId, userId)
            ?: throw IllegalArgumentException("Transaction not found")
    }

    @Transactional
    fun getTransactionsByDateRange(
        userId: UUID,
        startDate: LocalDateTime,
        endDate: LocalDateTime,
        pageable: Pageable
    ): Page<Transaction> {
        return transactionRepository.findAllByUserIdAndTransactionDateBetween(userId, startDate, endDate, pageable)
    }

    @Transactional
    fun getAll(userId: UUID, pageable: Pageable): Page<Transaction> {
        return transactionRepository.findAllByUserId(userId, pageable)
    }

    @Transactional
    fun deleteTransaction(userId: UUID, transactionId: UUID) {
        val transaction = transactionRepository.findByIdAndUserId(transactionId, userId)
            ?: throw IllegalArgumentException("Transaction not found")

        transactionRepository.deleteById(transactionId)
    }
}