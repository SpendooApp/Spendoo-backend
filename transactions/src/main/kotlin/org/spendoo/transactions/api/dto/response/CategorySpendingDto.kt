package org.spendoo.transactions.api.dto.response
import org.spendoo.transactions.entity.CategoryIcon
import java.math.BigDecimal

data class CategorySpendingDto(
    val categoryName: String,
    val categoryIcon: CategoryIcon,
    val totalAmount: BigDecimal
)