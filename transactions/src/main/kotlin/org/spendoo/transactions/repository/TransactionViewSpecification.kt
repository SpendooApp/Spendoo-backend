package org.spendoo.transactions.repository

import jakarta.persistence.criteria.JoinType
import jakarta.persistence.criteria.Predicate
import org.spendoo.transactions.entity.TransactionView
import org.springframework.data.jpa.domain.Specification
import java.math.BigDecimal
import java.util.*

object TransactionViewSpecification {

    fun buildSearchSpecification(userId: UUID, search: String?): Specification<TransactionView> {
        return Specification { root, _, criteriaBuilder ->
            val predicates = mutableListOf<Predicate>()

            predicates.add(criteriaBuilder.equal(root.get<UUID>("userId"), userId))

            if (!search.isNullOrBlank()) {
                val searchPattern = "%${search.lowercase()}%"
                val searchPredicates = mutableListOf<Predicate>()

                searchPredicates.add(
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("title")), searchPattern)
                )

                searchPredicates.add(
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("note")), searchPattern)
                )

                val categoryJoin = root.join<Any, Any>("category", JoinType.LEFT)
                searchPredicates.add(
                    criteriaBuilder.like(criteriaBuilder.lower(categoryJoin.get("categoryName")), searchPattern)
                )

                try {
                    val amount = BigDecimal(search)
                    searchPredicates.add(criteriaBuilder.equal(root.get<BigDecimal>("amount"), amount))
                    searchPredicates.add(criteriaBuilder.equal(root.get<BigDecimal>("amount"), amount.negate()))
                } catch (_: NumberFormatException) {
                }

                predicates.add(criteriaBuilder.or(*searchPredicates.toTypedArray()))
            }

            criteriaBuilder.and(*predicates.toTypedArray())
        }
    }
}
