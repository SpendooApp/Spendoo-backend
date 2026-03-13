package org.spendoo.categories.service

import org.spendoo.categories.api.dto.request.CategoryCreateRequest
import org.spendoo.categories.api.dto.request.CategoryUpdateRequest
import org.spendoo.categories.api.dto.request.toEntity
import org.spendoo.categories.api.dto.response.CategoryResponse
import org.spendoo.categories.api.dto.response.toResponse
import org.spendoo.categories.repository.CategoryRepository
import org.spendoo.categories.service.model.CategoryParams
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
class CategoryService(
    private val categoryRepository: CategoryRepository,
    private val budgetService: BudgetService
) {

    @Transactional
    fun create(request: CategoryCreateRequest, userId: UUID): CategoryResponse {
        val category = request.toEntity(userId)
        val savedCategory = categoryRepository.save(category)
        val budget = budgetService.createBudget(request.budget, savedCategory)
        return savedCategory.toResponse(budget)
    }

    fun getById(categoryId: UUID, userId: UUID): CategoryParams {
        return categoryRepository.getExistCategoryWithBudget(categoryId, userId)
            ?: throw IllegalArgumentException("Category not found")
    }

    fun getAll(userId: UUID, pageable: Pageable): Page<CategoryParams> {
        return categoryRepository.getExistCategoriesWithBudget(userId, pageable)
    }

    @Transactional
    fun update(categoryId: UUID, request: CategoryUpdateRequest, userId: UUID) {
        val category =
            categoryRepository.findByIdAndUserIdAndIsDeletedFalse(categoryId, userId)
                ?: throw IllegalArgumentException("Category not found")

        val updatedCategory = category.copy(
            categoryName = request.categoryName ?: category.categoryName,
            categoryIcon = request.categoryIcon ?: category.categoryIcon,
            leftOverOptions = request.leftOverOptions ?: category.leftOverOptions,
            priority = request.priority ?: category.priority,
        )
        categoryRepository.save(updatedCategory)
        request.budget?.let { budgetService.updateBudget(request.budget, category) }
    }

    @Transactional
    fun delete(categoryId: UUID, userId: UUID) {
        val category = categoryRepository.findByIdAndUserIdAndIsDeletedFalse(categoryId, userId)
            ?: throw IllegalArgumentException("Category not found")

        val updatedCategory = category.copy(isDeleted = true)
        categoryRepository.save(updatedCategory)
    }
}