package com.taskflow.auth

import com.taskflow.user.UserResponse
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.Instant

data class RegisterRequest(
    @field:NotBlank(message = "Name is required")
    @field:Size(max = 100, message = "Name must be at most 100 characters")
    val name: String = "",

    @field:NotBlank(message = "Email is required")
    @field:Email(message = "Must be a valid email")
    val email: String = "",

    @field:NotBlank(message = "Password is required")
    @field:Size(min = 8, max = 72, message = "Password must be 8-72 characters")
    val password: String = "",
)

data class LoginRequest(
    @field:NotBlank(message = "Email is required") val email: String = "",
    @field:NotBlank(message = "Password is required") val password: String = "",
)

data class AuthResponse(val token: String, val expiresAt: Instant, val user: UserResponse)
