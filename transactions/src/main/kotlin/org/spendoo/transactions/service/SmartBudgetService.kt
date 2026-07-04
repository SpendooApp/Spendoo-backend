package org.spendoo.transactions.service

import org.slf4j.LoggerFactory
import org.spendoo.client.ApiClient
import org.spendoo.events.notifications.NotificationDetails
import org.spendoo.events.notifications.UserNotificationsEvent
import org.spendoo.events.notifications.utils.NotificationMedium
import org.spendoo.events.notifications.utils.NotificationType
import org.spendoo.events.publisher.SpendooEventPublisher
import org.spendoo.events.transactions.ExpenseSavedEvent
import org.spendoo.transactions.api.dto.response.AiForecastResponse
import org.spendoo.transactions.entity.ActionType
import org.spendoo.transactions.entity.Category
import org.spendoo.transactions.entity.ProposedAction
import org.spendoo.transactions.repository.BudgetRepository
import org.spendoo.transactions.repository.CategoryRepository
import org.spendoo.transactions.repository.ProposedActionRepository
import org.springframework.context.event.EventListener
import org.springframework.data.domain.PageRequest
import org.springframework.http.HttpMethod
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.util.UUID

@Service
class SmartBudgetService(
    private val proposedActionRepository: ProposedActionRepository,
    private val categoryRepository: CategoryRepository,
    private val budgetRepository: BudgetRepository,
    private val publisher: SpendooEventPublisher,
    private val apiClient: ApiClient
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Async
    @EventListener
    fun onExpenseSaved(event: ExpenseSavedEvent) {
        checkAndForecastBudget(event.userId, event.categoryId)
    }

    fun checkAndForecastBudget(userId: UUID, categoryId: UUID) {
        try {
            val category = categoryRepository.findById(categoryId).orElseThrow {
                IllegalArgumentException("Category not found")
            }

            val activeBudget = budgetRepository.findByCategoryIdAndIsActiveIsTrue(categoryId)
                ?: return

            val startDate = when {
                activeBudget.period <= 7 -> activeBudget.startDate.minusWeeks(2)
                activeBudget.period <= 31 -> activeBudget.startDate.minusMonths(2)
                else -> activeBudget.startDate.minusMonths(6)
            }.withHour(0).withMinute(0)

            val endDate = activeBudget.endDate

            val forecastGranularity = when {
                activeBudget.period <= 7 -> "DAY"
                activeBudget.period <= 31 -> "DAY"
                else -> "MONTH"
            }
            val requestBody = mapOf(
                "user_id" to userId.toString(),
                "category_id" to categoryId.toString(),
                "granularity" to forecastGranularity,
                "start_date" to startDate.toString(),
                "end_date" to endDate.toString()
            )

            val response = apiClient.call(AiForecastResponse::class.java) {
                callAIService = true
                path = "/forecasting/predict"
                method = HttpMethod.POST
                body = requestBody
            } ?: throw IllegalStateException("Failed to get prediction from AI service")

            if (!response.predict) {
                log.info("AI could not make a reliable prediction. Aborting forecast check for user $userId.")
                return
            }

            for (bucket in response.buckets) {
                if (bucket.spending > bucket.budget) {
                    val overspentAmount = bucket.spending - bucket.budget

                    if (bucket.predicted) {
                        sendWarningNotification(userId, category, bucket.startDate)
                    } else {
                        suggestSmartAction(userId, category, overspentAmount)
                    }
                    break
                }
            }
        } catch (e: Exception) {
            log.error("Error analyzing category forecast for AI", e)
        }
    }

    private fun sendWarningNotification(userId: UUID, category: Category, date: String) {

        val notification = NotificationDetails(
            userId = userId,
            subject = "Budget Warning!",
            message = "Based on your spending habits, you might exceed your ${category.categoryName} budget around ${date}.",
            type = NotificationType.ALERT,
            medium = NotificationMedium.PUSH
        )
        publisher.publish(UserNotificationsEvent(listOf(notification)))
    }

    private fun suggestSmartAction(userId: UUID, currentCategory: Category, requiredAmount: BigDecimal) {
        val alternativeCategory = categoryRepository.findFirstByUserIdAndPriorityLessThanAndLeftoverGreaterThanOrderByPriorityAsc(
            userId, currentCategory.priority, requiredAmount,
            pageable = PageRequest.of(0, 1)
        ).firstOrNull()

        if (alternativeCategory != null) {
            val actionDataMap = mapOf(
                "fromCategoryId" to alternativeCategory.id,
                "toCategoryId" to currentCategory.id,
                "amount" to requiredAmount
            )

            val action = ProposedAction(
                userId = userId,
                type = ActionType.BUDGET_TRANSFER,
                actionData = actionDataMap
            )
            val savedAction = proposedActionRepository.save(action)

            val notification = NotificationDetails(
                userId = userId,
                subject = "Budget Exceeded!",
                message = "You overspent in ${currentCategory.categoryName}. Move $requiredAmount EGP from ${alternativeCategory.categoryName} to cover it?",
                type = NotificationType.ALERT,
                medium = NotificationMedium.PUSH,
                dataPayload = mapOf(
                    "actionId" to savedAction.id.toString(),
                    "actionType" to ActionType.BUDGET_TRANSFER.name,
                    "amount" to requiredAmount.toString()
                )
            )
            publisher.publish(UserNotificationsEvent(listOf(notification)))
        }
    }

    @Transactional
    fun executeAction(userId: UUID, actionId: UUID) {
        val action = proposedActionRepository.findById(actionId)
            .orElseThrow { IllegalArgumentException("Action not found") }

        if (action.userId != userId) {
            throw IllegalStateException("Invalid user for this action")
        }

        if (action.type == ActionType.BUDGET_TRANSFER) {
            val fromCatId = UUID.fromString(action.actionData["fromCategoryId"].toString())
            val toCatId = UUID.fromString(action.actionData["toCategoryId"].toString())
            val amount = BigDecimal(action.actionData["amount"].toString())

            val fromBudget = budgetRepository.findByCategoryIdAndIsActiveIsTrue(fromCatId)
            val toBudget = budgetRepository.findByCategoryIdAndIsActiveIsTrue(toCatId)

            if (fromBudget != null && toBudget != null) {
                val updatedFromBudget = fromBudget.copy(amount = fromBudget.amount - amount)
                val updatedToBudget = toBudget.copy(amount = toBudget.amount + amount)

                budgetRepository.saveAll(listOf(updatedFromBudget, updatedToBudget))
            }
        }

        proposedActionRepository.delete(action)
    }
}