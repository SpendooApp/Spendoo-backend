package org.spendoo.transactions.service

import org.spendoo.transactions.api.dto.request.CategoryCreateRequest
import org.spendoo.transactions.api.dto.request.CategoryUpdateRequest
import org.spendoo.transactions.api.dto.request.toEntity
import org.spendoo.transactions.api.dto.response.CategoryResponse
import org.spendoo.transactions.api.dto.response.toResponse
import org.spendoo.transactions.entity.Category
import org.spendoo.transactions.entity.CategoryIcon
import org.spendoo.transactions.entity.LeftOverOptions
import org.spendoo.transactions.repository.CategoryRepository
import org.spendoo.transactions.service.model.CategoriesSummary
import org.spendoo.transactions.service.model.CategoryParams
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.*

@Service
class CategoryService(
    private val categoryRepository: CategoryRepository,
    private val budgetService: BudgetService
) {

    @Transactional
    fun create(request: CategoryCreateRequest, userId: UUID) {
        val category = request.toEntity(userId)
        val savedCategory = categoryRepository.save(category)
        budgetService.createBudget(request.budget, savedCategory)
    }


    fun getById(categoryId: UUID, userId: UUID): CategoryResponse {

        val categoryParams = categoryRepository.getExistCategoryWithBudgetAndSpending(categoryId, userId)
            ?: throw IllegalArgumentException("Category not found")
        return categoryParams.toResponse()
    }
    
    fun getAll(userId: UUID, pageable: Pageable): Page<CategoryResponse> {
        val categoriesPage = categoryRepository.getExistCategoriesWithBudgetAndSpending(userId, pageable)
        return categoriesPage.map (CategoryParams::toResponse)
    }

    @Transactional
    fun update(categoryId: UUID, request: CategoryUpdateRequest, userId: UUID) {
        val category =
            categoryRepository.findByIdAndUserIdAndIsDeletedFalse(categoryId, userId)
                ?: throw IllegalArgumentException("Category not found")

        val updatedCategory = category.copy(
            categoryName = request.categoryName,
            categoryIcon = request.categoryIcon,
            leftOverOptions = request.leftOverOptions,
            priority = request.priority
        )

        categoryRepository.save(updatedCategory)
        budgetService.updateBudget(request.budget, category)
    }

    @Transactional
    fun createDefaultCategoriesForUser(userId: UUID) {
        val defaultCategories = listOf(
            Category(
                userId = userId,
                categoryName = "Food",
                categoryIcon = CategoryIcon.FOOD,
                leftOverOptions = LeftOverOptions.RESET_TO_ORIGINAL_AMOUNT,
                priority = 2
            ),
            Category(
                userId = userId,
                categoryName = "Transport",
                categoryIcon = CategoryIcon.TRANSPORT,
                leftOverOptions = LeftOverOptions.RESET_TO_ORIGINAL_AMOUNT,
                priority = 2
            ),
            Category(
                userId = userId,
                categoryName = "Shopping",
                categoryIcon = CategoryIcon.SHOPPING,
                leftOverOptions = LeftOverOptions.RESET_TO_ORIGINAL_AMOUNT,
                priority = 2
            ),
            Category(
                userId = userId,
                categoryName = "HealthCare",
                categoryIcon = CategoryIcon.HEALTHCARE,
                leftOverOptions = LeftOverOptions.RESET_TO_ORIGINAL_AMOUNT,
                priority = 3
            ),
            Category(
                userId = userId,
                categoryName = "Entertainment",
                categoryIcon = CategoryIcon.ENTERTAINMENT,
                leftOverOptions = LeftOverOptions.RESET_TO_ORIGINAL_AMOUNT,
                priority = 2
            ),
            Category(
                userId = userId,
                categoryName = "Other",
                categoryIcon = CategoryIcon.DEFAULT,
                leftOverOptions = LeftOverOptions.RESET_TO_ORIGINAL_AMOUNT,
                priority = 1
            )
        )

        categoryRepository.saveAll(defaultCategories)
            .forEach { budgetService.createZeroBudget(it, LocalDateTime.now()) }
    }

    @Transactional
    fun delete(categoryId: UUID, userId: UUID) {
        val category = categoryRepository.findByIdAndUserIdAndIsDeletedFalse(categoryId, userId)
            ?: throw IllegalArgumentException("Category not found")

        val updatedCategory = category.copy(isDeleted = true)
        categoryRepository.save(updatedCategory)
        budgetService.deactivateBudgetForCategory(categoryId)
    }

    fun getSummary(userId: UUID): CategoriesSummary {
        return categoryRepository.getCategoriesSummaryForUser(userId)
    }
}
