package com.taskflow.task.sort

import com.taskflow.task.Task
import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.Order
import jakarta.persistence.criteria.Root
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.domain.Specification

/**
 * Strategy pattern: each sort option knows how to order tasks.
 * Adding a new sort = adding one @Component; nothing else changes.
 */
interface TaskSortStrategy {
    /** Value of the `sort` query parameter, e.g. "dueDate". */
    val key: String

    fun orders(root: Root<Task>, cb: CriteriaBuilder, direction: Sort.Direction): List<Order>
}

/** Wraps a strategy as a Specification that only adds ORDER BY (and skips the page-count query). */
fun TaskSortStrategy.toSpecification(direction: Sort.Direction) = Specification<Task> { root, query, cb ->
    if (query != null && query.resultType != Long::class.javaObjectType) {
        query.orderBy(orders(root, cb, direction) + cb.asc(root.get<Long>("id")))
    }
    null
}

internal fun CriteriaBuilder.ordered(expression: jakarta.persistence.criteria.Expression<*>, direction: Sort.Direction): Order =
    if (direction.isAscending) asc(expression) else desc(expression)
