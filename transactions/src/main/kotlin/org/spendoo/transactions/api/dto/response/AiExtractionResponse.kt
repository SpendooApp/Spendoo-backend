package org.spendoo.transactions.api.dto.response

import com.fasterxml.jackson.annotation.JsonProperty
import java.math.BigDecimal
import java.util.UUID

data class AiExtractionResponse(
    val items: List<AiExtractionItem> = emptyList(),
    @field:JsonProperty("grand_total")
    val grandTotal: BigDecimal? = null,
    @field:JsonProperty("ocr_model")
    val model: String? = null,
)

data class AiExtractionItem(
    val id: Long? = null,
    @field:JsonProperty("item_name")
    val itemName: String? = null,
    val price: BigDecimal? = null,
    val category: String? = null,
    @field:JsonProperty("category_id")
    val categoryId: UUID? = null
)
