package org.spendoo.transactions.service

import org.spendoo.transactions.api.dto.request.CreateExpenseTransactionRequest
import org.spendoo.transactions.api.dto.request.CreateIncomeTransactionRequest
import org.spendoo.transactions.api.dto.request.TransactionUpdateRequest
import org.spendoo.transactions.api.dto.request.toEntity
import org.spendoo.transactions.api.dto.response.BalanceSummary
import org.spendoo.transactions.api.dto.response.CategorySpendingDto
import org.spendoo.transactions.entity.Transaction
import org.spendoo.transactions.repository.CategoryRepository
import org.spendoo.transactions.repository.TransactionRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.*

@Service
class TransactionService(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository
) {

    @Transactional
    fun createExpenseTransactions(userId: UUID, request: CreateExpenseTransactionRequest) {

        val transactionsToSave = request.entries.map { entry ->
            val category =
                categoryRepository.findByIdAndUserIdAndIsDeletedFalse(entry.categoryId, userId)
                    ?: throw IllegalArgumentException("Category not found with ID: ${entry.categoryId}")
            entry.toEntity(userId, category)
        }

        transactionRepository.saveAll(transactionsToSave)
    }

    @Transactional
    fun createIncomeTransactions(userId: UUID, request: CreateIncomeTransactionRequest) {
        val transactionsToSave = request.entries.map { it.toEntity(userId) }
        transactionRepository.saveAll(transactionsToSave)
    }

    @Transactional
    fun updateTransaction(transactionId: UUID, userId: UUID, updateRequest: TransactionUpdateRequest) {
        val transaction = transactionRepository.findByIdAndUserId(transactionId, userId)
            ?: throw IllegalArgumentException("Transaction not found")

        if (transaction.amount < BigDecimal.ZERO && updateRequest.categoryId == null) {
            throw IllegalArgumentException("Expense transactions must have a category")
        }

        val category = updateRequest.categoryId?.let {
            categoryRepository.findByIdAndUserIdAndIsDeletedFalse(it, userId)
                ?: throw IllegalArgumentException("Category not found with ID: $it")
        }

        val updatedTransaction = updateRequest.toEntity(transactionId, userId, category)

        transactionRepository.save(updatedTransaction)
    }

    @Transactional(readOnly = true)
    fun getTransactionById(transactionId: UUID, userId: UUID): Transaction {
        return transactionRepository.findByIdAndUserId(transactionId, userId)
            ?: throw IllegalArgumentException("Transaction not found")
    }

    @Transactional(readOnly = true)
    fun getTransactionsByDateRange(
        userId: UUID,
        startDate: LocalDateTime,
        endDate: LocalDateTime,
        pageable: Pageable
    ): Page<Transaction> {
        return transactionRepository.findAllByUserIdAndTransactionDateBetween(userId, startDate, endDate, pageable)
    }

    @Transactional(readOnly = true)
    fun getAll(userId: UUID, pageable: Pageable): Page<Transaction> {
        return transactionRepository.findAllByUserId(userId, pageable)
    }

    @Transactional
    fun deleteTransaction(userId: UUID, transactionId: UUID) {
        transactionRepository.findByIdAndUserId(transactionId, userId)
            ?: throw IllegalArgumentException("Transaction not found")

        transactionRepository.deleteById(transactionId)
    }

    fun getBalanceSummary(userId: UUID): BalanceSummary {

        val budgets = categoryRepository.sumActiveBudget(userId) ?: BigDecimal.ZERO

        val income = budgets + (transactionRepository.sumIncomeByUserId(userId)
            ?: BigDecimal.ZERO)

        val expenses = transactionRepository.sumExpensesByUserId(userId)
            ?: BigDecimal.ZERO

        val totalBalance = income.plus(expenses)

        return BalanceSummary(
            totalBalance = totalBalance,
            income = income,
            expenses = -expenses
        )
    }


    fun getTopSpendingCategories(userId: UUID, pageable: Pageable): Page<CategorySpendingDto> {
        return transactionRepository.findTopSpendingCategories(userId, pageable)
    }
}