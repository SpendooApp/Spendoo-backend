package org.spendoo.statistics.model

import java.time.LocalDateTime

data class IntervalRange(
    val label: String,
    val start: LocalDateTime,
    val end: LocalDateTime
)
