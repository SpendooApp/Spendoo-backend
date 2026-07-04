package org.spendoo.transactions.api.controller

import org.spendoo.transactions.service.SmartBudgetService
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1/budget-actions")
class BudgetActionController(
    private val smartBudgetService: SmartBudgetService
) {
    @PostMapping("/{actionId}/execute")
    fun executeProposedAction(
        @AuthenticationPrincipal userId: UUID,
        @PathVariable actionId: UUID
    ): ResponseEntity<Unit> {
        smartBudgetService.executeAction(userId, actionId)
        return ResponseEntity.ok().build()
    }
}