package com.taskflow.project

import com.taskflow.security.AuthenticatedUser
import com.taskflow.task.TaskService
import com.taskflow.task.TaskSummaryResponse
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/projects")
class ProjectController(
    private val projectService: ProjectService,
    private val taskService: TaskService,
) {

    @GetMapping
    fun list(@AuthenticationPrincipal user: AuthenticatedUser): List<ProjectResponse> =
        projectService.listForOwner(user.id)

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(
        @AuthenticationPrincipal user: AuthenticatedUser,
        @Valid @RequestBody req: CreateProjectRequest,
    ): ProjectResponse = projectService.create(user.id, req)

    @GetMapping("/{id}")
    fun get(@AuthenticationPrincipal user: AuthenticatedUser, @PathVariable id: Long): ProjectResponse =
        projectService.get(id, user.id)

    @GetMapping("/{id}/summary")
    fun summary(@AuthenticationPrincipal user: AuthenticatedUser, @PathVariable id: Long): TaskSummaryResponse =
        taskService.summary(id, user.id)
}
