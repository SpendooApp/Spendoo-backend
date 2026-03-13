package org.spendoo.categories.service

import org.spendoo.categories.api.dto.request.CategoryCreateRequest
import org.spendoo.categories.api.dto.response.CategoryResponse
import org.spendoo.categories.entity.CategoryIcon
import org.spendoo.categories.entity.LeftOverOptions
import org.spendoo.categories.mapper.CategoryMapper
import org.spendoo.categories.mapper.toEntity
import org.spendoo.categories.repository.BudgetRepository
import org.spendoo.categories.repository.CategoryRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
class CategoryService(
    private val categoryRepository: CategoryRepository,
    private val budgetRepository: BudgetRepository,
    private val categoryMapper: CategoryMapper,
    private val budgetService: BudgetService
) {

    @Transactional
    fun create(request: CategoryCreateRequest, userId: UUID): CategoryResponse {
        val category = request.toEntity(userId)
        val savedCategory = categoryRepository.save(category)
        val budget = budgetService.createBudget(request.budget, savedCategory)
        return categoryMapper.toResponse(savedCategory, budget)
    }

    fun getById(categoryId: UUID, userId: UUID): CategoryResponse {
        val category =
            categoryRepository.findById(categoryId).orElseThrow { IllegalArgumentException("Category not found") }
        if (category.userId != userId || category.isDeleted) {
            throw IllegalArgumentException("Category not found")
        }
        val budget = budgetRepository.findByCategoryCategoryId(categoryId)
        return categoryMapper.toResponse(category, budget)
    }

    fun getAll(userId: UUID, pageable: Pageable): Page<CategoryResponse> {
        val categoriesPage = categoryRepository.findAllByUserIdOrUserIdIsNullAndIsDeletedFalse(userId, pageable)
        return categoriesPage.map { category ->
            val budget = budgetRepository.findByCategoryCategoryId(category.categoryId)
            categoryMapper.toResponse(category, budget)
        }
    }

    @Transactional
    fun update(categoryId: UUID, request: CategoryCreateRequest, userId: UUID): CategoryResponse {
        val category =
            categoryRepository.findById(categoryId).orElseThrow { IllegalArgumentException("Category not found") }
        if (category.userId != userId || category.isDeleted) {
            throw IllegalArgumentException("Category not found")
        }
        val updatedCategory = category.copy(
            categoryName = request.categoryName,
            categoryIcon = CategoryIcon.valueOf(request.categoryIcon.uppercase()),
            leftOverOptions = LeftOverOptions.valueOf(request.leftOverOptions.uppercase()),
            priority = request.priority
        )
        val savedCategory = categoryRepository.save(updatedCategory)
        val budget = budgetRepository.findByCategoryCategoryId(categoryId)
            ?: throw IllegalArgumentException("Budget not found")

        val updatedBudget = budgetService.updateBudget(request.budget, budget)
        return categoryMapper.toResponse(savedCategory, updatedBudget)
    }

    @Transactional
    fun delete(categoryId: UUID, userId: UUID) {
        val category =
            categoryRepository.findById(categoryId).orElseThrow { IllegalArgumentException("Category not found") }
        if (category.userId != userId || category.isDeleted) {
            throw IllegalArgumentException("Category not found")
        }
        val updatedCategory = category.copy(isDeleted = true)
        categoryRepository.save(updatedCategory)
    }
}