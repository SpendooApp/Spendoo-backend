package org.spendoo.app.controller

import org.spendoo.app.dto.budgetDtos.BudgetCreateRequest
import org.spendoo.app.dto.budgetDtos.BudgetDto
import org.spendoo.app.dto.budgetDtos.BudgetUpdateRequest
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController


@RestController
@RequestMapping("/api/budgets")
class BudgetController {

    private val mock = mutableListOf(
        BudgetDto(1, 1, "Food", 2000.0, 1500.0, "monthly", "2026-01-01","2026-01-31",false),
        BudgetDto(2, 2, "Travel", 3000.0, 500.0, "monthly", "2026-01-02","2026-01-31",false),
        BudgetDto(3, 3, "Shopping", 5000.0, 2500.0, "monthly", "2026-01-01","2026-01-31",false),
        BudgetDto(4, 4, "Loan", 10000.0, 6500.0, "monthly", "2026-01-02","2026-01-31",false),
        BudgetDto(5, 5, "Car", 15000.0, 8300.0, "monthly", "2026-01-01","2026-01-31",false),
        BudgetDto(6, 6, "Health", 7000.0, 950.0, "monthly", "2026-01-01","2026-01-31",false)
    )

    @GetMapping
    fun list(): List<BudgetDto> = mock

    @GetMapping("/{id}")
    fun get(@PathVariable id: Long): BudgetDto =
        mock.find {it.id == id} ?: BudgetDto(id,0, "Unknown", 0.0, 0.0, "Unknown", "","",false)

    @PostMapping
    fun create(@RequestBody req: BudgetCreateRequest): BudgetDto {
        val created = BudgetDto(
            id = (mock.maxOfOrNull { it.id }?:0) +1,
            categoryId = req.categoryId,
            categoryName = "Category-${req.categoryId}",
            amount = req.amount,
            spent = 0.0,
            period = req.period,
            startDate = req.startDate,
            endDate = req.endDate,
            carryOver = req.carryOver
        )
        mock.add(created)
        return created
    }

    @PutMapping("/{id}")
    fun update(@PathVariable id: Long, @RequestBody req: BudgetUpdateRequest): BudgetDto {
        val old = mock.find { it.id == id } ?: BudgetDto(id,0, "Unknown", 0.0, 0.0, "Unknown", "","",false)
        val new = old.copy(
            amount = req.amount?:old.amount,
            spent = req.spent?:old.spent,
        )
        mock.removeIf { it.id == id }
        mock.add(new)
        return new
    }

}