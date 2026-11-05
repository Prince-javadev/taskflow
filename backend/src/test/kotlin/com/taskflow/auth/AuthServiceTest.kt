package com.taskflow.auth

import com.taskflow.common.ConflictException
import com.taskflow.common.UnauthorizedException
import com.taskflow.security.IssuedToken
import com.taskflow.security.JwtService
import com.taskflow.user.User
import com.taskflow.user.UserRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.security.crypto.password.PasswordEncoder
import java.time.Instant

class AuthServiceTest {

    private val userRepository = mockk<UserRepository>()
    private val passwordEncoder = mockk<PasswordEncoder>()
    private val jwtService = mockk<JwtService>()
    private val service = AuthService(userRepository, passwordEncoder, jwtService)

    @Test
    fun `register normalises the email and stores a hashed password`() {
        every { userRepository.existsByEmail("jay@test.com") } returns false
        every { passwordEncoder.encode("Secret123!") } returns "hashed"
        every { userRepository.save(any<User>()) } answers { firstArg<User>().apply { id = 1 } }

        val result = service.register(RegisterRequest(name = " Jay ", email = " Jay@Test.com ", password = "Secret123!"))

        assertEquals("jay@test.com", result.email)
        assertEquals("Jay", result.name)
        verify { userRepository.save(match<User> { it.passwordHash == "hashed" }) }
    }

    @Test
    fun `register rejects an email that is already taken`() {
        every { userRepository.existsByEmail("jay@test.com") } returns true

        assertThrows<ConflictException> {
            service.register(RegisterRequest(name = "Jay", email = "jay@test.com", password = "Secret123!"))
        }
        verify(exactly = 0) { userRepository.save(any<User>()) }
    }

    @Test
    fun `login fails with the same message for a wrong password`() {
        val user = User(name = "Jay", email = "jay@test.com", passwordHash = "hashed").apply { id = 1 }
        every { userRepository.findByEmail("jay@test.com") } returns user
        every { passwordEncoder.matches("wrong", "hashed") } returns false

        val ex = assertThrows<UnauthorizedException> {
            service.login(LoginRequest(email = "jay@test.com", password = "wrong"))
        }
        assertEquals("Invalid email or password", ex.message)
    }

    @Test
    fun `login returns a token for valid credentials`() {
        val user = User(name = "Jay", email = "jay@test.com", passwordHash = "hashed").apply { id = 1 }
        every { userRepository.findByEmail("jay@test.com") } returns user
        every { passwordEncoder.matches("Secret123!", "hashed") } returns true
        every { jwtService.generate(user) } returns IssuedToken("token-123", Instant.parse("2026-01-01T01:00:00Z"))

        val result = service.login(LoginRequest(email = "JAY@test.com", password = "Secret123!"))

        assertEquals("token-123", result.token)
        assertEquals(1L, result.user.id)
    }
}
