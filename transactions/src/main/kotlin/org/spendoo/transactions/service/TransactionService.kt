package org.spendoo.transactions.service

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
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
        val requestedCategoryIds = request.entries.map { it.categoryId }.toSet()
        val categoriesById = categoryRepository
            .findAllByIdInAndUserIdAndIsDeletedFalse(requestedCategoryIds, userId)
            .associateBy { it.id }

        val transactionsToSave = request.entries.map { entry ->
            val category = categoriesById[entry.categoryId]
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

        if (transaction.amount >= BigDecimal.ZERO && updateRequest.categoryId != null) {
            throw IllegalArgumentException("Income transactions cannot have a category")
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
        if (transactionRepository.deleteByIdAndUserId(transactionId, userId) == 0)
            throw IllegalArgumentException("Transaction not found")
    }

    suspend fun getBalanceSummary(userId: UUID): BalanceSummary = coroutineScope {
        // Run blocking JPA calls on IO dispatcher so they can execute in parallel.
        val budgetsDeferred = async(Dispatchers.IO) { categoryRepository.sumActiveBudget(userId) ?: BigDecimal.ZERO }
        val incomeDeferred = async(Dispatchers.IO) { transactionRepository.sumIncomeByUserId(userId) ?: BigDecimal.ZERO }
        val expensesDeferred = async(Dispatchers.IO) { transactionRepository.sumExpensesByUserId(userId) ?: BigDecimal.ZERO }

        val budgets = budgetsDeferred.await()
        val income = budgets + incomeDeferred.await()
        val expenses = expensesDeferred.await()

        val totalBalance = income.plus(expenses)

        return@coroutineScope BalanceSummary(
            totalBalance = totalBalance,
            income = income,
            expenses = -expenses
        )
    }


    fun getTopSpendingCategories(userId: UUID, pageable: Pageable): Page<CategorySpendingDto> {
        return transactionRepository.findTopSpendingCategories(userId, pageable)
    }
}