package org.spendoo.transactions.api.controller

import jakarta.validation.Valid
import org.spendoo.transactions.api.dto.request.CategoryCreateRequest
import org.spendoo.transactions.api.dto.request.CategoryUpdateRequest
import org.spendoo.transactions.api.dto.response.CategoryResponse
import org.spendoo.transactions.api.dto.response.toResponse
import org.spendoo.transactions.service.CategoryService
import org.spendoo.transactions.service.model.CategoryParams
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
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
        @Valid @RequestBody request: CategoryCreateRequest, @AuthenticationPrincipal userId: UUID
    ): ResponseEntity<Unit> {
        categoryService.create(request, userId)
        return ResponseEntity.status(HttpStatus.CREATED).build()
    }

    @GetMapping("/{categoryId}")
    fun getCategoryById(
        @PathVariable categoryId: UUID, @AuthenticationPrincipal userId: UUID
    ): ResponseEntity<CategoryResponse> {
        val category = categoryService.getById(categoryId, userId)
        return ResponseEntity.ok(category.toResponse())
    }

    @GetMapping
    fun getAllCategories(
        @AuthenticationPrincipal userId: UUID, pageable: Pageable
    ): ResponseEntity<Page<CategoryResponse>> {
        val page = categoryService.getAll(userId, pageable)
        return ResponseEntity.ok(page.map(CategoryParams::toResponse))
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
    fun deleteCategory(@PathVariable categoryId: UUID, @AuthenticationPrincipal userId: UUID): ResponseEntity<Void> {
        categoryService.delete(categoryId, userId)
        return ResponseEntity.ok().build()
    }
}