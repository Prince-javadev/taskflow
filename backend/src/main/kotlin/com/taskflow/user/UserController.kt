package com.taskflow.user

import org.springframework.data.domain.Sort
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/** Lists users so the frontend can offer an assignee dropdown. */
@RestController
@RequestMapping("/api/users")
class UserController(private val userRepository: UserRepository) {

    @GetMapping
    fun list(): List<UserResponse> =
        userRepository.findAll(Sort.by("name")).map { it.toResponse() }
}
