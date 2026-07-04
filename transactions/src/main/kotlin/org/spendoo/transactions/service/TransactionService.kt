package org.spendoo.transactions.service

import org.spendoo.client.ApiClient
import org.spendoo.events.publisher.SpendooEventPublisher
import org.spendoo.events.transactions.ExpenseSavedEvent
import org.spendoo.events.transactions.TransactionCreatedEvent
import org.spendoo.transactions.api.dto.request.CreateExpenseTransactionRequest
import org.spendoo.transactions.api.dto.request.CreateIncomeTransactionRequest
import org.spendoo.transactions.api.dto.request.TransactionUpdateRequest
import org.spendoo.transactions.api.dto.request.toEntity
import org.spendoo.transactions.api.dto.response.*
import org.spendoo.transactions.entity.PlanCode
import org.spendoo.transactions.entity.Transaction
import org.spendoo.transactions.entity.TransactionView
import org.spendoo.transactions.entity.UserAIUsage
import org.spendoo.transactions.repository.CategoryRepository
import org.spendoo.transactions.repository.TransactionRepository
import org.spendoo.transactions.repository.TransactionViewRepository
import org.spendoo.transactions.repository.UserAIUsageRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.server.ResponseStatusException
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

@Service
class TransactionService(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val transactionViewRepository: TransactionViewRepository,
    private val userAIUsageRepository: UserAIUsageRepository,
    private val apiClient: ApiClient,
    private val spendooEventPublisher: SpendooEventPublisher,
    private val categoryService: CategoryService
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
        val distinctCategoryCount = transactionRepository.countDistinctCategoriesByUserId(userId).toInt()
        spendooEventPublisher.publish(TransactionCreatedEvent(userId, distinctCategoryCount))

        requestedCategoryIds.forEach { categoryId ->
            spendooEventPublisher.publish(ExpenseSavedEvent(userId, categoryId))
        }
    }

    @Transactional
    fun createIncomeTransactions(userId: UUID, request: CreateIncomeTransactionRequest) {
        val transactionsToSave = request.entries.map { it.toEntity(userId) }
        transactionRepository.saveAll(transactionsToSave)
        val distinctCategoryCount = transactionRepository.countDistinctCategoriesByUserId(userId).toInt()
        spendooEventPublisher.publish(TransactionCreatedEvent(userId, distinctCategoryCount))
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

        updateRequest.categoryId?.let { categoryId ->
            spendooEventPublisher.publish(ExpenseSavedEvent(userId, categoryId))
        }
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
        val income = transactionRepository.sumIncomeByUserId(userId) ?: BigDecimal.ZERO
        val expenses = transactionRepository.sumExpensesByUserId(userId) ?: BigDecimal.ZERO

        val realIncome = budgets + income

        val totalBalance = realIncome.plus(expenses)

        return BalanceSummary(
            totalBalance = totalBalance,
            income = realIncome,
            expenses = -expenses
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
        validateUsage(userId, isOcr = false)
        val response = apiClient.call(AiExtractionResponse::class.java) {
            callAIService = true
            path = "/api/v1/voice/process/$userId"
            method = HttpMethod.POST
            header("Content-Type", "multipart/form-data")
            val multiValueMap = LinkedMultiValueMap<String, Any>()
            multiValueMap.add("file", file.resource)
            body = multiValueMap
        }
        if (response != null) {
            incrementUsage(userId, isOcr = false)
        }
        return enrichAiExtractionResponse(response, userId)
    }

    fun processOcrTransaction(file: MultipartFile, userId: UUID): EnrichedAiExtractionResponse? {
        validateUsage(userId, isOcr = true)
        val response = apiClient.call(AiExtractionResponse::class.java) {
            callAIService = true
            path = "/api/v1/ocr/scan/$userId"
            method = HttpMethod.POST
            header("Content-Type", "multipart/form-data")
            val multiValueMap = LinkedMultiValueMap<String, Any>()
            multiValueMap.add("file", file.resource)
            body = multiValueMap
        }
        if (response != null) {
            incrementUsage(userId, isOcr = true)
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

    private fun validateUsage(userId: UUID, isOcr: Boolean) {
        val currentPlan = categoryService.getCurrentPlanCode(userId)
        var usage = userAIUsageRepository.findByUserId(userId) ?: UserAIUsage(userId = userId)

        if (LocalDate.now().isAfter(usage.resetDate)) {
            usage = usage.copy(ocrCount = 0, voiceCount = 0, resetDate = LocalDate.now().plusMonths(1))
            userAIUsageRepository.save(usage)
        }

        val currentCount = if (isOcr) usage.ocrCount else usage.voiceCount

        val limit = when (currentPlan) {
            PlanCode.FREE -> if (isOcr) 5 else 10
            PlanCode.BASIC -> if (isOcr) 30 else 50
            PlanCode.PRO -> Int.MAX_VALUE
        }

        if (currentCount >= limit) {
            throw ResponseStatusException(HttpStatus.PAYMENT_REQUIRED)
        }
    }

    private fun incrementUsage(userId: UUID, isOcr: Boolean) {

        val usage = userAIUsageRepository.findByUserId(userId) ?: UserAIUsage(userId = userId)

        val updatedUsage = if (isOcr) {
            usage.copy(ocrCount = usage.ocrCount + 1)
        } else {
            usage.copy(voiceCount = usage.voiceCount + 1)
        }
        userAIUsageRepository.save(updatedUsage)
    }

}
