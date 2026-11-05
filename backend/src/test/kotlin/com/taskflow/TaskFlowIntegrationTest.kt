package com.taskflow

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.patch
import org.springframework.test.web.servlet.post
import org.springframework.transaction.annotation.Transactional

/**
 * End-to-end through HTTP, security, services, JPA and the real PostgreSQL schema (Flyway).
 * Requires the database from docker-compose. @Transactional rolls every test back.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TaskFlowIntegrationTest(
    @Autowired private val mockMvc: MockMvc,
    @Autowired private val objectMapper: ObjectMapper,
) {

    private fun login(email: String, password: String): String {
        val body = mockMvc.post("/api/auth/login") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"email":"$email","password":"$password"}"""
        }.andExpect { status { isOk() } }.andReturn().response.contentAsString
        return "Bearer " + objectMapper.readTree(body)["token"].asText()
    }

    private fun createProject(token: String, name: String): Long {
        val body = mockMvc.post("/api/projects") {
            header(HttpHeaders.AUTHORIZATION, token)
            contentType = MediaType.APPLICATION_JSON
            content = """{"name":"$name"}"""
        }.andExpect { status { isCreated() } }.andReturn().response.contentAsString
        return objectMapper.readTree(body)["id"].asLong()
    }

    @Test
    fun `owner creates a project and task, moves it to done and sees it in the summary`() {
        val token = login("demo@taskflow.dev", "Password123!")
        val projectId = createProject(token, "Integration project")

        val taskBody = mockMvc.post("/api/projects/$projectId/tasks") {
            header(HttpHeaders.AUTHORIZATION, token)
            contentType = MediaType.APPLICATION_JSON
            content = """{"title":"Integration task","priority":"HIGH"}"""
        }.andExpect {
            status { isCreated() }
            jsonPath("$.status") { value("TODO") }
        }.andReturn().response.contentAsString
        val taskId = objectMapper.readTree(taskBody)["id"].asLong()

        mockMvc.patch("/api/tasks/$taskId/status") {
            header(HttpHeaders.AUTHORIZATION, token)
            contentType = MediaType.APPLICATION_JSON
            content = """{"status":"DONE"}"""
        }.andExpect { status { isOk() } }

        mockMvc.get("/api/projects/$projectId/summary") {
            header(HttpHeaders.AUTHORIZATION, token)
        }.andExpect {
            status { isOk() }
            jsonPath("$.counts.DONE") { value(1) }
            jsonPath("$.counts.TODO") { value(0) }
            jsonPath("$.total") { value(1) }
            jsonPath("$.overdue") { value(0) }
        }
    }

    @Test
    fun `a user cannot see another user's project`() {
        val ownerToken = login("demo@taskflow.dev", "Password123!")
        val projectId = createProject(ownerToken, "Private project")

        mockMvc.post("/api/auth/register") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"name":"Intruder","email":"intruder@test.com","password":"Secret123!"}"""
        }.andExpect { status { isCreated() } }
        val intruderToken = login("intruder@test.com", "Secret123!")

        mockMvc.get("/api/projects/$projectId/tasks") {
            header(HttpHeaders.AUTHORIZATION, intruderToken)
        }.andExpect { status { isNotFound() } }
    }
}
