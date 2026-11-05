package com.taskflow.task

import com.taskflow.project.Project
import com.taskflow.user.User
import org.springframework.data.jpa.domain.Specification

/** Specification pattern: each filter is a small, composable query predicate. */
object TaskSpecifications {

    fun inProject(projectId: Long) = Specification<Task> { root, _, cb ->
        cb.equal(root.get<Project>("project").get<Long>("id"), projectId)
    }

    fun hasStatus(status: TaskStatus?) = Specification<Task> { root, _, cb ->
        status?.let { cb.equal(root.get<TaskStatus>("status"), it) }
    }

    fun hasPriority(priority: TaskPriority?) = Specification<Task> { root, _, cb ->
        priority?.let { cb.equal(root.get<TaskPriority>("priority"), it) }
    }

    fun assignedTo(assigneeId: Long?) = Specification<Task> { root, _, cb ->
        assigneeId?.let { cb.equal(root.get<User>("assignee").get<Long>("id"), it) }
    }

    fun matching(projectId: Long, filter: TaskFilter): Specification<Task> = Specification.allOf(
        inProject(projectId),
        hasStatus(filter.status),
        hasPriority(filter.priority),
        assignedTo(filter.assigneeId),
    )
}
