package org.spendoo.transactions.api.dto.response
import org.spendoo.transactions.entity.CategoryIcon
import java.math.BigDecimal
import java.util.UUID

data class CategorySpendingDto(
    val id: UUID,
    val categoryName: String,
    val categoryIcon: CategoryIcon,
    val totalAmount: BigDecimal
)