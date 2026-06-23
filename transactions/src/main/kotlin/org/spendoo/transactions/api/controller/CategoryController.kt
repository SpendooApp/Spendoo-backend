package org.spendoo.transactions.api.controller

import jakarta.validation.Valid
import org.spendoo.transactions.api.dto.request.CategoryCreateRequest
import org.spendoo.transactions.api.dto.request.CategoryUpdateRequest
import org.spendoo.transactions.api.dto.response.CategoryResponse
import org.spendoo.transactions.api.dto.response.CategorySpendingDto
import org.spendoo.transactions.service.CategoryService
import org.spendoo.transactions.service.model.CategoriesSummary
import org.springdoc.core.annotations.ParameterObject
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
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
    ): ResponseEntity<Unit> {
        categoryService.create(request, userId)
        return ResponseEntity.status(HttpStatus.CREATED).build()
    }

    @GetMapping("/{categoryId}")
    fun getCategoryById(
        @PathVariable categoryId: UUID,
        @AuthenticationPrincipal userId: UUID
    ): ResponseEntity<CategoryResponse> {
        val category = categoryService.getById(categoryId, userId)
        return ResponseEntity.ok(category)
    }

    @GetMapping
    fun getAllCategories(
        @AuthenticationPrincipal userId: UUID,
        @ParameterObject pageable: Pageable
    ): ResponseEntity<Page<CategoryResponse>> {
        val page = categoryService.getAll(userId, pageable)
        return ResponseEntity.ok(page)
    }


    @PatchMapping("/{categoryId}")
    fun updateCategory(
        @PathVariable categoryId: UUID,
        @Valid @RequestBody request: CategoryUpdateRequest,
        @AuthenticationPrincipal userId: UUID
    ): ResponseEntity<Unit> {
        categoryService.update(categoryId, request, userId)
        return ResponseEntity.ok().build()
    }

    @DeleteMapping("/{categoryId}")
    fun deleteCategory(
        @PathVariable categoryId: UUID,
        @AuthenticationPrincipal userId: UUID
    ): ResponseEntity<Unit> {
        categoryService.delete(categoryId, userId)
        return ResponseEntity.ok().build()
    }

    @GetMapping("/summary")
    fun getCategoriesSummary(
        @AuthenticationPrincipal userId: UUID
    ): ResponseEntity<CategoriesSummary> {
        val summary = categoryService.getSummary(userId)
        return ResponseEntity.ok(summary)
    }

    @GetMapping("/top-spending")
    fun getTopSpending(
        @AuthenticationPrincipal userId: UUID,
        @PageableDefault(size = 5)
        pageable: Pageable,
    ): ResponseEntity<Page<CategorySpendingDto>> {
        val topSpending = categoryService.getTopSpendingCategories(userId, pageable)
        return ResponseEntity.ok(topSpending)
    }
}