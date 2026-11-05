package com.taskflow.user

data class UserResponse(val id: Long, val name: String, val email: String)

fun User.toResponse() = UserResponse(id = requireNotNull(id), name = name, email = email)
