package com.taskflow.auth

import com.taskflow.common.ConflictException
import com.taskflow.common.UnauthorizedException
import com.taskflow.security.JwtService
import com.taskflow.user.User
import com.taskflow.user.UserRepository
import com.taskflow.user.UserResponse
import com.taskflow.user.toResponse
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtService: JwtService,
) {

    @Transactional
    fun register(req: RegisterRequest): UserResponse {
        val email = req.email.trim().lowercase()
        if (userRepository.existsByEmail(email)) throw ConflictException("Email is already registered")
        val user = userRepository.save(
            User(name = req.name.trim(), email = email, passwordHash = passwordEncoder.encode(req.password))
        )
        return user.toResponse()
    }

    @Transactional(readOnly = true)
    fun login(req: LoginRequest): AuthResponse {
        val user = userRepository.findByEmail(req.email.trim().lowercase())
        if (user == null || !passwordEncoder.matches(req.password, user.passwordHash)) {
            throw UnauthorizedException("Invalid email or password")
        }
        val issued = jwtService.generate(user)
        return AuthResponse(issued.token, issued.expiresAt, user.toResponse())
    }
}
