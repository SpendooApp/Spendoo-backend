package org.spendoo.category_management.repository

import org.spendoo.category_management.entity.Category
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface CategoryRepository : JpaRepository<Category, UUID> {
    fun findAllByUserIdOrUserIdIsNullAndIsDeletedFalse(userId: UUID, pageable: Pageable): Page<Category>
}