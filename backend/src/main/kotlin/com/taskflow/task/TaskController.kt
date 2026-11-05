package com.taskflow.task

import com.taskflow.common.PageResponse
import com.taskflow.security.AuthenticatedUser
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api")
class TaskController(private val taskService: TaskService) {

    /**
     * Examples:
     *  /api/projects/1/tasks?status=TODO&priority=HIGH
     *  /api/projects/1/tasks?assigneeId=2&sort=dueDate&direction=asc&page=0&size=20
     *  sort = createdAt (default) | dueDate | priority
     */
    @GetMapping("/projects/{projectId}/tasks")
    fun list(
        @AuthenticationPrincipal user: AuthenticatedUser,
        @PathVariable projectId: Long,
        @RequestParam(required = false) status: TaskStatus?,
        @RequestParam(required = false) priority: TaskPriority?,
        @RequestParam(required = false) assigneeId: Long?,
        @RequestParam(defaultValue = "createdAt") sort: String,
        @RequestParam(defaultValue = "asc") direction: String,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): PageResponse<TaskResponse> =
        taskService.list(projectId, user.id, TaskFilter(status, priority, assigneeId), sort, direction, page, size)

    @PostMapping("/projects/{projectId}/tasks")
    @ResponseStatus(HttpStatus.CREATED)
    fun create(
        @AuthenticationPrincipal user: AuthenticatedUser,
        @PathVariable projectId: Long,
        @Valid @RequestBody req: CreateTaskRequest,
    ): TaskResponse = taskService.create(projectId, user.id, req)

    @PutMapping("/tasks/{id}")
    fun update(
        @AuthenticationPrincipal user: AuthenticatedUser,
        @PathVariable id: Long,
        @Valid @RequestBody req: UpdateTaskRequest,
    ): TaskResponse = taskService.update(id, user.id, req)

    @PatchMapping("/tasks/{id}/status")
    fun updateStatus(
        @AuthenticationPrincipal user: AuthenticatedUser,
        @PathVariable id: Long,
        @Valid @RequestBody req: UpdateTaskStatusRequest,
    ): TaskResponse = taskService.updateStatus(id, user.id, req)

    @DeleteMapping("/tasks/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(@AuthenticationPrincipal user: AuthenticatedUser, @PathVariable id: Long) =
        taskService.delete(id, user.id)
}
