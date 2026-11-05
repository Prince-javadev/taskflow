package com.taskflow.task

import com.taskflow.user.toResponse
import org.springframework.stereotype.Component
import java.time.Clock
import java.time.LocalDate

/** Mapper/Factory: the only place that turns a Task entity into its API representation. */
@Component
class TaskMapper(private val clock: Clock) {

    fun toResponse(task: Task): TaskResponse {
        val today = LocalDate.now(clock)
        return TaskResponse(
            id = requireNotNull(task.id),
            projectId = requireNotNull(task.project.id),
            title = task.title,
            description = task.description,
            status = task.status,
            priority = task.priority,
            dueDate = task.dueDate,
            overdue = isOverdue(task, today),
            assignee = task.assignee?.toResponse(),
            createdAt = requireNotNull(task.createdAt),
            updatedAt = requireNotNull(task.updatedAt),
        )
    }

    companion object {
        fun isOverdue(task: Task, today: LocalDate): Boolean =
            task.status != TaskStatus.DONE && task.dueDate?.isBefore(today) == true
    }
}
