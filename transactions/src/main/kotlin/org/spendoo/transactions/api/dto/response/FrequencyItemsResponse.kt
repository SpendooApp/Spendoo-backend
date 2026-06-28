package org.spendoo.transactions.api.dto.response

import org.spendoo.transactions.entity.CategoryIcon
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.UUID

data class FrequencyItemsResponse(
    val itemName : String,
    val frequency: Long,
    val totalAmount: BigDecimal,
    val categoryId : UUID,
    val categoryIcon: CategoryIcon,
)
{
    val averageAmount: BigDecimal
        get() = if (frequency > 0) {
            totalAmount.abs().divide(BigDecimal.valueOf(frequency), 2, RoundingMode.HALF_UP)
        } else {
            BigDecimal.ZERO
        }
}
