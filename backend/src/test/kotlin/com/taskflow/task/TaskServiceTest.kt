package com.taskflow.task

import com.taskflow.common.BadRequestException
import com.taskflow.common.FieldValidationException
import com.taskflow.common.NotFoundException
import com.taskflow.project.Project
import com.taskflow.project.ProjectService
import com.taskflow.task.sort.CreatedAtSortStrategy
import com.taskflow.task.sort.DueDateSortStrategy
import com.taskflow.task.sort.PrioritySortStrategy
import com.taskflow.task.sort.TaskSortResolver
import com.taskflow.user.User
import com.taskflow.user.UserRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.Optional

class TaskServiceTest {

    private val taskRepository = mockk<TaskRepository>()
    private val projectService = mockk<ProjectService>()
    private val userRepository = mockk<UserRepository>()

    /** "Today" is fixed so date rules are deterministic. */
    private val clock = Clock.fixed(Instant.parse("2026-06-15T10:00:00Z"), ZoneOffset.UTC)
    private val today = LocalDate.parse("2026-06-15")

    private val service = TaskService(
        taskRepository = taskRepository,
        projectService = projectService,
        userRepository = userRepository,
        taskMapper = TaskMapper(clock),
        sortResolver = TaskSortResolver(listOf(DueDateSortStrategy(), PrioritySortStrategy(), CreatedAtSortStrategy())),
        clock = clock,
    )

    private val owner = User(name = "Owner", email = "owner@test.com", passwordHash = "x").apply { id = 1 }
    private val project = Project(name = "Project", description = null, owner = owner).apply { id = 10 }

    private fun task(status: TaskStatus = TaskStatus.TODO, dueDate: LocalDate? = null) =
        Task(
            title = "Task", description = null, status = status, priority = TaskPriority.LOW,
            dueDate = dueDate, assignee = null, project = project,
        ).apply {
            id = 7
            createdAt = Instant.now(clock)
            updatedAt = Instant.now(clock)
        }

    @Test
    fun `create rejects a due date in the past`() {
        every { projectService.requireOwned(10, 1) } returns project

        val ex = assertThrows<FieldValidationException> {
            service.create(10, 1, CreateTaskRequest(title = "T", priority = TaskPriority.HIGH, dueDate = today.minusDays(1)))
        }

        assertEquals("dueDate", ex.field)
        verify(exactly = 0) { taskRepository.save(any<Task>()) }
    }

    @Test
    fun `create defaults status to TODO, trims the title and accepts today as due date`() {
        every { projectService.requireOwned(10, 1) } returns project
        val saved = slot<Task>()
        every { taskRepository.save(capture(saved)) } answers {
            saved.captured.apply {
                id = 99
                createdAt = Instant.now(clock)
                updatedAt = Instant.now(clock)
            }
        }

        val result = service.create(
            10, 1, CreateTaskRequest(title = "  Write tests  ", priority = TaskPriority.MEDIUM, dueDate = today),
        )

        assertEquals(TaskStatus.TODO, result.status)
        assertEquals("Write tests", result.title)
        assertFalse(result.overdue)
    }

    @Test
    fun `create rejects an assignee that does not exist`() {
        every { projectService.requireOwned(10, 1) } returns project
        every { userRepository.findById(42) } returns Optional.empty()

        val ex = assertThrows<FieldValidationException> {
            service.create(10, 1, CreateTaskRequest(title = "T", priority = TaskPriority.LOW, assigneeId = 42))
        }
        assertEquals("assigneeId", ex.field)
    }

    @Test
    fun `updateStatus returns 404 for a task the user does not own`() {
        every { taskRepository.findByIdAndProjectOwnerId(7, 2) } returns null

        assertThrows<NotFoundException> {
            service.updateStatus(7, 2, UpdateTaskStatusRequest(TaskStatus.DONE))
        }
    }

    @Test
    fun `updateStatus changes only the status`() {
        val existing = task(status = TaskStatus.TODO)
        every { taskRepository.findByIdAndProjectOwnerId(7, 1) } returns existing
        every { taskRepository.saveAndFlush(existing) } returns existing

        val result = service.updateStatus(7, 1, UpdateTaskStatusRequest(TaskStatus.DONE))

        assertEquals(TaskStatus.DONE, result.status)
        assertEquals("Task", result.title)
    }

    @Test
    fun `summary fills missing statuses with zero and reports overdue count`() {
        every { projectService.requireOwned(10, 1) } returns project
        every { taskRepository.countByStatus(10) } returns listOf(statusCount(TaskStatus.TODO, 3))
        every { taskRepository.countByProjectIdAndDueDateBeforeAndStatusNot(10, today, TaskStatus.DONE) } returns 2

        val summary = service.summary(10, 1)

        assertEquals(mapOf(TaskStatus.TODO to 3L, TaskStatus.IN_PROGRESS to 0L, TaskStatus.DONE to 0L), summary.counts)
        assertEquals(3L, summary.total)
        assertEquals(2L, summary.overdue)
    }

    @Test
    fun `list rejects an unknown sort key`() {
        every { projectService.requireOwned(10, 1) } returns project

        assertThrows<BadRequestException> {
            service.list(10, 1, TaskFilter(), sort = "banana", direction = "asc", page = 0, size = 20)
        }
    }

    @Test
    fun `a task is overdue only when its due date has passed and it is not done`() {
        assertTrue(TaskMapper.isOverdue(task(TaskStatus.TODO, today.minusDays(1)), today))
        assertFalse(TaskMapper.isOverdue(task(TaskStatus.DONE, today.minusDays(1)), today))
        assertFalse(TaskMapper.isOverdue(task(TaskStatus.TODO, today), today))
        assertFalse(TaskMapper.isOverdue(task(TaskStatus.TODO, null), today))
    }

    private fun statusCount(taskStatus: TaskStatus, total: Long) = object : StatusCount {
        override val status = taskStatus
        override val count = total
    }
}
