package org.spendoo.savingGoals.repository

import org.spendoo.savingGoals.entity.Achievement
import org.springframework.data.jpa.repository.JpaRepository
import java.math.BigDecimal
import java.util.*

interface AchievementRepository : JpaRepository<Achievement, UUID> {
    fun findByTargetValue(targetValue: BigDecimal): Achievement?
    fun findByTitleEn(titleEn: String): Achievement?
}