package com.taskflow.task

import com.taskflow.user.UserResponse
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.time.Instant
import java.time.LocalDate

data class CreateTaskRequest(
    @field:NotBlank(message = "Title is required")
    @field:Size(max = 120, message = "Title must be at most 120 characters")
    val title: String = "",

    @field:Size(max = 2000, message = "Description must be at most 2000 characters")
    val description: String? = null,

    @field:NotNull(message = "Priority is required")
    val priority: TaskPriority? = null,

    /** Defaults to TODO when omitted. */
    val status: TaskStatus? = null,

    /** Must not be in the past (checked in TaskService using the injected Clock). */
    val dueDate: LocalDate? = null,

    val assigneeId: Long? = null,
)

data class UpdateTaskRequest(
    @field:NotBlank(message = "Title is required")
    @field:Size(max = 120, message = "Title must be at most 120 characters")
    val title: String = "",

    @field:Size(max = 2000, message = "Description must be at most 2000 characters")
    val description: String? = null,

    @field:NotNull(message = "Priority is required")
    val priority: TaskPriority? = null,

    @field:NotNull(message = "Status is required")
    val status: TaskStatus? = null,

    val dueDate: LocalDate? = null,

    val assigneeId: Long? = null,
)

data class UpdateTaskStatusRequest(
    @field:NotNull(message = "Status is required")
    val status: TaskStatus? = null,
)

data class TaskResponse(
    val id: Long,
    val projectId: Long,
    val title: String,
    val description: String?,
    val status: TaskStatus,
    val priority: TaskPriority,
    val dueDate: LocalDate?,
    val overdue: Boolean,
    val assignee: UserResponse?,
    val createdAt: Instant,
    val updatedAt: Instant,
)

data class TaskSummaryResponse(
    val projectId: Long,
    val counts: Map<TaskStatus, Long>,
    val total: Long,
    val overdue: Long,
)

/** Filters accepted by GET /api/projects/{id}/tasks. All optional. */
data class TaskFilter(
    val status: TaskStatus? = null,
    val priority: TaskPriority? = null,
    val assigneeId: Long? = null,
)
