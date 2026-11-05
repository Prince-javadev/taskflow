package com.taskflow.common

import java.time.Instant

data class ApiError(
    val timestamp: Instant,
    val status: Int,
    val error: String,
    val message: String,
    val path: String,
    val fieldErrors: List<FieldError> = emptyList(),
) {
    data class FieldError(val field: String, val message: String)
}
