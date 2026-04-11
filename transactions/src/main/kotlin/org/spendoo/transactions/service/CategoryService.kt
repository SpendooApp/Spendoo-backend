package org.spendoo.transactions.service

import org.spendoo.transactions.api.dto.request.CategoryCreateRequest
import org.spendoo.transactions.api.dto.request.CategoryUpdateRequest
import org.spendoo.transactions.api.dto.request.toEntity
import org.spendoo.transactions.api.dto.response.CategoryResponse
import org.spendoo.transactions.api.dto.response.toResponse
import org.spendoo.transactions.repository.CategoryRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
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
        request.budget?.let {
            budgetService.createBudget(request.budget, savedCategory)
        }
    }


    fun getById(categoryId: UUID, userId: UUID): CategoryResponse {

        val categoryParams = categoryRepository.getExistCategoryWithBudget(categoryId, userId)
            ?: throw IllegalArgumentException("Category not found")
        val spentAmount = budgetService.calculateSpentAmount(userId, categoryId)
        return categoryParams.toResponse(spentAmount = spentAmount)
    }


    // fun getAll(userId: UUID, pageable: Pageable): Page<CategoryResponse> {
    //     return categoryRepository.getExistCategoriesWithBudget(userId, pageable)
    //         .map { categoryParams ->
    //             val spentAmount = budgetService.calculateSpentAmount(userId, categoryParams.categoryId)
    //             categoryParams.toResponse(spentAmount = spentAmount)
    //         }
    // }

    
    fun getAll(userId: UUID, pageable: Pageable): Page<CategoryResponse> {
        val categoriesPage = categoryRepository.getExistCategoriesWithBudget(userId, pageable)

        val categoryIds = categoriesPage.content.map { it.categoryId }
        val spentAmountsMap = budgetService.calculateSpentAmountsForCategories(userId, categoryIds)
        
        return categoriesPage.map { categoryParams ->
            val spentAmount = spentAmountsMap[categoryParams.categoryId] ?: BigDecimal.ZERO
            categoryParams.toResponse(spentAmount = spentAmount)
        }
    }

    @Transactional
    fun update(categoryId: UUID, request: CategoryUpdateRequest, userId: UUID) {
        val category =
            categoryRepository.findByIdAndUserIdAndIsDeletedFalse(categoryId, userId)
                ?: throw IllegalArgumentException("Category not found")

        val updatedCategory = request.toEntity(categoryId = categoryId, userId = userId)

        categoryRepository.save(updatedCategory)
        request.budget?.let { budgetService.updateBudget(request.budget, category) }
    }

    @Transactional
    fun delete(categoryId: UUID, userId: UUID) {
        val category = categoryRepository.findByIdAndUserIdAndIsDeletedFalse(categoryId, userId)
            ?: throw IllegalArgumentException("Category not found")

        val updatedCategory = category.copy(isDeleted = true)
        categoryRepository.save(updatedCategory)
        budgetService.deactivateBudgetForCategory(categoryId)
    }
}
