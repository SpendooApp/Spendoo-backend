package org.spendoo.categories.entity

import jakarta.persistence.*
import java.util.*

@Entity
@Table(name = "categories", schema = "categories")
data class Category(
    @Id
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    val categoryId: UUID = UUID.randomUUID(),

    @Column(columnDefinition = "uuid", nullable = false)
    val userId: UUID = UUID.randomUUID(),

    @Column(nullable = false)
    val categoryName: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val categoryIcon: CategoryIcon,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val leftOverOptions: LeftOverOptions,

    @Column(nullable = false)
    val priority: Int,

    @Column(nullable = false)
    val isDeleted: Boolean = false,

    @OneToMany(mappedBy = "category", cascade = [CascadeType.ALL], fetch = FetchType.LAZY, orphanRemoval = true)
    val budgets: MutableList<Budget> = mutableListOf()
)
