package org.spendoo.categories.service.model

import org.spendoo.categories.entity.CategoryIcon
import org.spendoo.categories.entity.LeftOverOptions
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.*

class CategoryParams(
    val categoryId: UUID,

    val categoryName: String,

    val categoryIcon: CategoryIcon,

    val priority: Int,

    val leftOverOptions: LeftOverOptions,

    val budgetId: UUID?,

    val amount: BigDecimal?,

    val carryOver: BigDecimal?,

    val period: Int?,

    val startDate: LocalDateTime?,

    val endDate: LocalDateTime?
)