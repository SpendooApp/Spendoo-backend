package org.spendoo.savingGoals.api.dto.request

import jakarta.validation.constraints.Positive
import java.math.BigDecimal

data class AssignAmountRequest(
    @field:Positive(message = "Amount must be greater than 0")
    val amount: BigDecimal,
)
