package org.spendoo.app.controller

import org.spendoo.app.dto.transactionDtos.TransactionCreateRequest
import org.spendoo.app.dto.transactionDtos.TransactionDto
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/transactions")
class TransactionController {

    private val mock = mutableListOf<TransactionDto>()

    @PostMapping
    fun create(@RequestBody req: TransactionCreateRequest): List<TransactionDto> {
        if (req.amount.size != req.category.size) {
            throw IllegalArgumentException("Amounts and categories must have the same size")
        }
        val createdList = req.amount.zip(req.category).map{(amount,category)->
            val created = TransactionDto(
                id = (mock.maxOfOrNull { it.id } ?: 0) + 1,
                type = req.type,
                date = req.date,
                description = req.description,
                category = listOf(category),
                amount = listOf(amount),
                createdAt = java.time.LocalTime.now().toString()
            )
            mock.add(created)
            created
        }
        return createdList
    }







}