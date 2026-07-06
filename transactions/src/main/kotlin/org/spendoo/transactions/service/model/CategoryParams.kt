package org.spendoo.transactions.service.model

import org.spendoo.transactions.entity.CategoryIcon
import org.spendoo.transactions.entity.LeftOverOptions
import java.math.BigDecimal
import java.time.Instant
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

    val startDate: Instant?,

    val endDate: Instant?,

    val spentAmount: BigDecimal?
)