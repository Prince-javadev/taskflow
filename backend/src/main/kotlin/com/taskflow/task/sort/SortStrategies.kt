package com.taskflow.task.sort

import com.taskflow.task.Task
import com.taskflow.task.TaskPriority
import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.Order
import jakarta.persistence.criteria.Root
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Component
import java.time.Instant
import java.time.LocalDate

/** Due date, with tasks that have no due date always last. */
@Component
class DueDateSortStrategy : TaskSortStrategy {
    override val key = "dueDate"

    override fun orders(root: Root<Task>, cb: CriteriaBuilder, direction: Sort.Direction): List<Order> {
        val dueDate = root.get<LocalDate>("dueDate")
        val nullsLast = cb.selectCase<Int>().`when`(cb.isNull(dueDate), 1).otherwise(0)
        return listOf(cb.asc(nullsLast), cb.ordered(dueDate, direction))
    }
}

/** Priority by business rank (HIGH > MEDIUM > LOW), not alphabetically. Descending = most important first. */
@Component
class PrioritySortStrategy : TaskSortStrategy {
    override val key = "priority"

    override fun orders(root: Root<Task>, cb: CriteriaBuilder, direction: Sort.Direction): List<Order> {
        val priority = root.get<TaskPriority>("priority")
        var rank = cb.selectCase<Int>()
        TaskPriority.entries.forEach { p -> rank = rank.`when`(cb.equal(priority, p), p.rank) }
        return listOf(cb.ordered(rank.otherwise(0), direction))
    }
}

/** Default: creation time. */
@Component
class CreatedAtSortStrategy : TaskSortStrategy {
    override val key = "createdAt"

    override fun orders(root: Root<Task>, cb: CriteriaBuilder, direction: Sort.Direction): List<Order> =
        listOf(cb.ordered(root.get<Instant>("createdAt"), direction))
}
