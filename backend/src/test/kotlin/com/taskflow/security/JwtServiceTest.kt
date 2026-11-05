package com.taskflow.security

import com.taskflow.config.AppProperties
import com.taskflow.user.User
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class JwtServiceTest {

    private val props = AppProperties(
        jwt = AppProperties.Jwt(secret = "unit-test-secret-that-is-long-enough-123", expirationMinutes = 60),
        cors = AppProperties.Cors(allowedOrigins = emptyList()),
    )
    private val issuedAt = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC)
    private val user = User(name = "Asha", email = "asha@test.com", passwordHash = "x").apply { id = 5 }

    @Test
    fun `a generated token parses back to the same user`() {
        val service = JwtService(props, issuedAt)
        val token = service.generate(user).token

        assertEquals(AuthenticatedUser(5, "asha@test.com"), service.parse(token))
    }

    @Test
    fun `an expired token is rejected`() {
        val token = JwtService(props, issuedAt).generate(user).token
        val twoHoursLater = Clock.fixed(Instant.parse("2026-01-01T02:00:00Z"), ZoneOffset.UTC)

        assertNull(JwtService(props, twoHoursLater).parse(token))
    }

    @Test
    fun `a tampered token is rejected`() {
        val service = JwtService(props, issuedAt)
        val token = service.generate(user).token

        assertNull(service.parse(token.dropLast(2) + "xx"))
        assertNull(service.parse("not-a-jwt"))
    }
}
