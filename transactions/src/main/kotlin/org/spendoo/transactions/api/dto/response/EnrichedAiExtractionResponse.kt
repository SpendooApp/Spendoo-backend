package org.spendoo.transactions.api.dto.response

import org.spendoo.transactions.entity.CategoryIcon
import java.math.BigDecimal
import java.util.UUID

data class EnrichedAiExtractionResponse(
    val items: List<EnrichedAiExtractionItem> = emptyList(),
    val grandTotal: BigDecimal? = null,
    val model: String? = null,
)

data class EnrichedAiExtractionItem(
    val id: Long? = null,
    val itemName: String? = null,
    val price: BigDecimal? = null,
    val category: String? = null,
    val categoryId: UUID? = null,
    val categoryName: String? = null,
    val categoryIcon: CategoryIcon? = null
)
