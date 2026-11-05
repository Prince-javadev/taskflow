package com.taskflow.common

class NotFoundException(message: String) : RuntimeException(message)
class ConflictException(message: String) : RuntimeException(message)
class UnauthorizedException(message: String) : RuntimeException(message)
class BadRequestException(message: String) : RuntimeException(message)

/** A business-rule validation error tied to one request field (returned as a field-level 400). */
class FieldValidationException(val field: String, message: String) : RuntimeException(message)
