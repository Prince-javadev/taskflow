package com.taskflow.task

import com.taskflow.common.BadRequestException
import com.taskflow.common.FieldValidationException
import com.taskflow.common.NotFoundException
import com.taskflow.common.PageResponse
import com.taskflow.project.ProjectService
import com.taskflow.task.sort.TaskSortResolver
import com.taskflow.task.sort.toSpecification
import com.taskflow.user.User
import com.taskflow.user.UserRepository
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.LocalDate

@Service
class TaskService(
    private val taskRepository: TaskRepository,
    private val projectService: ProjectService,
    private val userRepository: UserRepository,
    private val taskMapper: TaskMapper,
    private val sortResolver: TaskSortResolver,
    private val clock: Clock,
) {

    @Transactional(readOnly = true)
    fun list(
        projectId: Long,
        ownerId: Long,
        filter: TaskFilter,
        sort: String,
        direction: String,
        page: Int,
        size: Int,
    ): PageResponse<TaskResponse> {
        projectService.requireOwned(projectId, ownerId)
        val strategy = sortResolver.resolve(sort)
        val spec = TaskSpecifications.matching(projectId, filter).and(strategy.toSpecification(parseDirection(direction)))
        val pageable = PageRequest.of(page.coerceAtLeast(0), size.coerceIn(1, MAX_PAGE_SIZE))
        return PageResponse.from(taskRepository.findAll(spec, pageable), taskMapper::toResponse)
    }

    @Transactional
    fun create(projectId: Long, ownerId: Long, req: CreateTaskRequest): TaskResponse {
        val project = projectService.requireOwned(projectId, ownerId)
        if (req.dueDate != null && req.dueDate.isBefore(today())) {
            throw FieldValidationException("dueDate", "Due date cannot be in the past")
        }
        val task = Task(
            title = req.title.trim(),
            description = req.description?.trim(),
            status = req.status ?: TaskStatus.TODO,
            priority = requireNotNull(req.priority),
            dueDate = req.dueDate,
            assignee = findAssignee(req.assigneeId),
            project = project,
        )
        return taskMapper.toResponse(taskRepository.save(task))
    }

    @Transactional
    fun update(taskId: Long, ownerId: Long, req: UpdateTaskRequest): TaskResponse {
        val task = requireOwnedTask(taskId, ownerId)
        task.title = req.title.trim()
        task.description = req.description?.trim()
        task.priority = requireNotNull(req.priority)
        task.status = requireNotNull(req.status)
        task.dueDate = req.dueDate
        task.assignee = findAssignee(req.assigneeId)
        return taskMapper.toResponse(taskRepository.saveAndFlush(task))
    }

    @Transactional
    fun updateStatus(taskId: Long, ownerId: Long, req: UpdateTaskStatusRequest): TaskResponse {
        val task = requireOwnedTask(taskId, ownerId)
        task.status = requireNotNull(req.status)
        return taskMapper.toResponse(taskRepository.saveAndFlush(task))
    }

    @Transactional
    fun delete(taskId: Long, ownerId: Long) {
        taskRepository.delete(requireOwnedTask(taskId, ownerId))
    }

    @Transactional(readOnly = true)
    fun summary(projectId: Long, ownerId: Long): TaskSummaryResponse {
        projectService.requireOwned(projectId, ownerId)
        val found = taskRepository.countByStatus(projectId).associate { it.status to it.count }
        val counts = TaskStatus.entries.associateWith { found[it] ?: 0L }
        val overdue = taskRepository.countByProjectIdAndDueDateBeforeAndStatusNot(projectId, today(), TaskStatus.DONE)
        return TaskSummaryResponse(projectId = projectId, counts = counts, total = counts.values.sum(), overdue = overdue)
    }

    private fun requireOwnedTask(taskId: Long, ownerId: Long): Task =
        taskRepository.findByIdAndProjectOwnerId(taskId, ownerId)
            ?: throw NotFoundException("Task $taskId not found")

    private fun findAssignee(assigneeId: Long?): User? =
        assigneeId?.let {
            userRepository.findByIdOrNull(it)
                ?: throw FieldValidationException("assigneeId", "User $it does not exist")
        }

    private fun parseDirection(direction: String): Sort.Direction =
        Sort.Direction.fromOptionalString(direction).orElseThrow {
            BadRequestException("Unsupported direction '$direction'. Allowed: asc, desc")
        }

    private fun today(): LocalDate = LocalDate.now(clock)

    companion object {
        const val MAX_PAGE_SIZE = 100
    }
}
