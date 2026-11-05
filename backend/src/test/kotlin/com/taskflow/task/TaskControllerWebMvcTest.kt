package com.taskflow.task

import com.ninjasquad.springmockk.MockkBean
import com.taskflow.config.AppProperties
import com.taskflow.config.SecurityConfig
import com.taskflow.config.TimeConfig
import com.taskflow.security.JwtService
import com.taskflow.user.User
import io.mockk.every
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.patch
import org.springframework.test.web.servlet.post
import java.time.Instant

/** Web-layer test: real security config, JWT filter, validation and error handler; service is mocked. */
@WebMvcTest(TaskController::class)
@Import(SecurityConfig::class, JwtService::class, TimeConfig::class)
@EnableConfigurationProperties(AppProperties::class)
@TestPropertySource(
    properties = [
        "app.jwt.secret=webmvc-test-secret-that-is-long-enough",
        "app.jwt.expiration-minutes=60",
        "app.cors.allowed-origins=http://localhost:5173",
    ],
)
class TaskControllerWebMvcTest(
    @Autowired private val mockMvc: MockMvc,
    @Autowired private val jwtService: JwtService,
) {

    @MockkBean
    private lateinit var taskService: TaskService

    private fun bearer(): String {
        val user = User(name = "Tester", email = "tester@test.com", passwordHash = "x").apply { id = 5 }
        return "Bearer ${jwtService.generate(user).token}"
    }

    @Test
    fun `rejects requests without a token with a JSON 401`() {
        mockMvc.get("/api/projects/1/tasks").andExpect {
            status { isUnauthorized() }
            jsonPath("$.message") { value("Missing, invalid or expired token") }
        }
    }

    @Test
    fun `returns field-level 400 errors for an invalid task`() {
        mockMvc.post("/api/projects/1/tasks") {
            header(HttpHeaders.AUTHORIZATION, bearer())
            contentType = MediaType.APPLICATION_JSON
            content = """{"title":"","priority":null}"""
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.message") { value("Validation failed") }
            jsonPath("$.fieldErrors[?(@.field == 'title')]") { exists() }
            jsonPath("$.fieldErrors[?(@.field == 'priority')]") { exists() }
        }

        verify(exactly = 0) { taskService.create(any(), any(), any()) }
    }

    @Test
    fun `updates status for the authenticated user`() {
        every { taskService.updateStatus(7, 5, any()) } returns TaskResponse(
            id = 7, projectId = 1, title = "Task", description = null,
            status = TaskStatus.DONE, priority = TaskPriority.HIGH, dueDate = null, overdue = false,
            assignee = null, createdAt = Instant.EPOCH, updatedAt = Instant.EPOCH,
        )

        mockMvc.patch("/api/tasks/7/status") {
            header(HttpHeaders.AUTHORIZATION, bearer())
            contentType = MediaType.APPLICATION_JSON
            content = """{"status":"DONE"}"""
        }.andExpect {
            status { isOk() }
            jsonPath("$.status") { value("DONE") }
        }
    }
}
