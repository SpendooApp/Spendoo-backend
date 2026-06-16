package org.spendoo.savingGoals.api.controller

import jakarta.validation.Valid
import org.spendoo.savingGoals.api.dto.request.AssignAmountRequest
import org.spendoo.savingGoals.api.dto.request.GoalCreateRequest
import org.spendoo.savingGoals.api.dto.request.GoalUpdateRequest
import org.spendoo.savingGoals.api.dto.response.GoalResponse
import org.spendoo.savingGoals.api.dto.response.GoalsSummary
import org.spendoo.savingGoals.service.SavingGoalService
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import java.util.*


@RestController
@RequestMapping("/api/v1/goals")
class SavingGoalController(
    private val savingGoalService: SavingGoalService,
) {

    @PostMapping
    fun createSavingGoal(
        @Valid @RequestBody request: GoalCreateRequest,
        @AuthenticationPrincipal userId: UUID
    ): ResponseEntity<Unit> {
        savingGoalService.createSavingGoal(request, userId)
        return ResponseEntity.status(HttpStatus.CREATED).build()
    }

    @PostMapping("/{goalId}/assign")
    fun assignAmountToGoal(
        @PathVariable goalId: UUID,
        @AuthenticationPrincipal userId: UUID,
        @Valid @RequestBody request: AssignAmountRequest
    ): ResponseEntity<Unit> {
        savingGoalService.assignAmountToGoal(goalId, userId, request)
        return ResponseEntity.ok().build()
    }

    @PostMapping("/add-to-savings")
    fun addAmountToSaving(
        @AuthenticationPrincipal userId: UUID,
        @Valid @RequestBody request: AssignAmountRequest
    ): ResponseEntity<Unit> {
        savingGoalService.addToSavings(userId, amount = request.amount)
        return ResponseEntity.ok().build()
    }

    @PatchMapping("/{goalId}")
    fun updateGoal(
        @PathVariable goalId: UUID,
        @Valid @RequestBody request: GoalUpdateRequest,
        @AuthenticationPrincipal userId: UUID
    ): ResponseEntity<Unit> {
        savingGoalService.updateGoal(goalId, request, userId)
        return ResponseEntity.ok().build()
    }

    @DeleteMapping("/{goalId}")
    fun deleteGoal(
        @PathVariable goalId: UUID,
        @AuthenticationPrincipal userId: UUID
    ): ResponseEntity<Unit> {
        savingGoalService.deleteGoal(userId, goalId)
        return ResponseEntity.ok().build()
    }

    @GetMapping("/{goalId}")
    fun getSavingGoalById(
        @PathVariable goalId: UUID,
        @AuthenticationPrincipal userId: UUID
    ): ResponseEntity<GoalResponse> {
        val goal = savingGoalService.getSavingGoalById(goalId, userId)
        return ResponseEntity.ok(goal)
    }

    @GetMapping
    fun getAll(
        @AuthenticationPrincipal userId: UUID,
        @PageableDefault(size = 10) pageable: Pageable
    ): ResponseEntity<Page<GoalResponse>> {
        val pageRequest = if (pageable.sort.isSorted) {
            pageable
        } else {
            val sort = Sort.by(Sort.Order.asc("isCompleted"), Sort.Order.desc("priority"))
            PageRequest.of(pageable.pageNumber, pageable.pageSize, sort)
        }

        val page = savingGoalService.getAllGoals(userId, pageRequest)
        return ResponseEntity.ok(page)
    }

    @GetMapping("/goals-summary")
    fun getGoalsSummary(
        @AuthenticationPrincipal userId: UUID
    ): ResponseEntity<GoalsSummary> {
        val summary = savingGoalService.getSummary(userId)
        return ResponseEntity.ok(summary)
    }

}