package org.spendoo.categories.api.controller

import jakarta.validation.Valid
import org.spendoo.categories.api.dto.request.CategoryCreateRequest
import org.spendoo.categories.api.dto.response.CategoryResponse
import org.spendoo.categories.service.CategoryService
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/api/v1/categories")
class CategoryController(
    private val categoryService: CategoryService
) {

    @PostMapping
    fun createCategory(
        @Valid @RequestBody request: CategoryCreateRequest,
        @AuthenticationPrincipal userId: UUID
    ): CategoryResponse {
        return categoryService.create(request, userId)
    }

    @GetMapping("/{categoryId}")
    fun getCategoryById(@PathVariable categoryId: UUID, @AuthenticationPrincipal userId: UUID): CategoryResponse {
        return categoryService.getById(categoryId, userId)
    }

    @GetMapping
    fun getAllCategories(@AuthenticationPrincipal userId: UUID, pageable: Pageable): Page<CategoryResponse> {
        return categoryService.getAll(userId, pageable)
    }


    @PutMapping("/{categoryId}")
    fun updateCategory(
        @PathVariable categoryId: UUID,
        @Valid @RequestBody request: CategoryCreateRequest,
        @AuthenticationPrincipal userId: UUID
    ): CategoryResponse {
        return categoryService.update(categoryId, request, userId)
    }

    @DeleteMapping("/{categoryId}")
    fun deleteCategory(@PathVariable categoryId: UUID, @AuthenticationPrincipal userId: UUID) {
        categoryService.delete(categoryId, userId)
    }
}