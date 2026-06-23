package org.spendoo.transactions.service

import org.spendoo.client.ApiClient
import org.spendoo.transactions.api.dto.request.CreateExpenseTransactionRequest
import org.spendoo.transactions.api.dto.request.CreateIncomeTransactionRequest
import org.spendoo.transactions.api.dto.request.TransactionUpdateRequest
import org.spendoo.transactions.api.dto.request.toEntity
import org.spendoo.transactions.api.dto.response.BalanceSummary
import org.spendoo.transactions.entity.Transaction
import org.spendoo.transactions.api.dto.response.AiExtractionResponse
import org.spendoo.transactions.api.dto.response.EnrichedAiExtractionItem
import org.spendoo.transactions.api.dto.response.EnrichedAiExtractionResponse
import org.spendoo.transactions.api.dto.response.FrequencyItemsResponse
import org.spendoo.transactions.entity.TransactionView
import org.spendoo.transactions.repository.CategoryRepository
import org.springframework.http.HttpMethod
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.multipart.MultipartFile
import org.spendoo.transactions.repository.TransactionRepository
import org.spendoo.transactions.repository.TransactionViewRepository
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
    private val categoryRepository: CategoryRepository,
    private val transactionViewRepository: TransactionViewRepository,
    private val apiClient: ApiClient
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
    fun getAll(userId: UUID, search: String?, pageable: Pageable): Page<TransactionView> {
        val spec = org.spendoo.transactions.repository.TransactionViewSpecification.buildSearchSpecification(userId, search)
        return transactionViewRepository.findAll(spec, pageable)
    }

    @Transactional
    fun deleteTransaction(userId: UUID, transactionId: UUID) {
        if (transactionRepository.deleteByIdAndUserId(transactionId, userId) == 0)
            throw IllegalArgumentException("Transaction not found")
    }

    fun getBalanceSummary(userId: UUID): BalanceSummary {
        val budgets = categoryRepository.sumActiveBudget(userId) ?: BigDecimal.ZERO
        val incomeDeferred = transactionRepository.sumIncomeByUserId(userId) ?: BigDecimal.ZERO
        val expensesDeferred = transactionRepository.sumExpensesByUserId(userId) ?: BigDecimal.ZERO

        val income = budgets + incomeDeferred

        val totalBalance = income.plus(expensesDeferred)

        return BalanceSummary(
            totalBalance = totalBalance,
            income = income,
            expenses = -expensesDeferred
        )
    }
    @Transactional(readOnly = true)
    fun getTopFrequencyItems(userId: UUID, categoryId: UUID?, pageable: Pageable): Page<FrequencyItemsResponse> {
        if (categoryId != null) {
            return transactionRepository.findTopSpendingItemsByCategoryId(userId, categoryId, pageable)
        }
        return transactionRepository.findTopSpendingItems(userId, pageable)
    }


    fun processVoiceTransaction(file: MultipartFile, userId: UUID): EnrichedAiExtractionResponse? {
        val response = apiClient.call(AiExtractionResponse::class.java) {
            callAIService = true
            path = "/api/v1/voice/process/$userId"
            method = HttpMethod.POST
            header("Content-Type", "multipart/form-data")
            val multiValueMap = LinkedMultiValueMap<String, Any>()
            multiValueMap.add("file", file.resource)
            body = multiValueMap
        }
        return enrichAiExtractionResponse(response, userId)
    }

    fun processOcrTransaction(file: MultipartFile, userId: UUID): EnrichedAiExtractionResponse? {
        val response = apiClient.call(AiExtractionResponse::class.java) {
            callAIService = true
            path = "/api/v1/ocr/scan/$userId"
            method = HttpMethod.POST
            header("Content-Type", "multipart/form-data")
            val multiValueMap = LinkedMultiValueMap<String, Any>()
            multiValueMap.add("file", file.resource)
            body = multiValueMap
        }
        return enrichAiExtractionResponse(response, userId)
    }

    private fun enrichAiExtractionResponse(response: AiExtractionResponse?, userId: UUID): EnrichedAiExtractionResponse? {
        if (response == null) return null
        val categoryIds = response.items.mapNotNull { it.categoryId }.distinct()

        val categoryMap = if (categoryIds.isNotEmpty()) {
            categoryRepository.findAllByIdInAndUserIdAndIsDeletedFalse(categoryIds, userId)
                .associateBy { it.id }
        } else {
            emptyMap()
        }

        val enrichedItems = response.items.map { item ->
            val category = item.categoryId?.let { categoryMap[it] }
            EnrichedAiExtractionItem(
                id = item.id,
                itemName = item.itemName,
                price = item.price,
                category = item.category,
                categoryId = item.categoryId,
                categoryName = category?.categoryName,
                categoryIcon = category?.categoryIcon
            )
        }
        return EnrichedAiExtractionResponse(
            items = enrichedItems,
            grandTotal = response.grandTotal,
            model = response.model
        )
    }
}