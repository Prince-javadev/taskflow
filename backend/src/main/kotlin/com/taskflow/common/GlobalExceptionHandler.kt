package com.taskflow.common

import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import org.springframework.web.servlet.resource.NoResourceFoundException
import java.time.Clock

@RestControllerAdvice
class GlobalExceptionHandler(private val clock: Clock) {

    private val log = LoggerFactory.getLogger(javaClass)

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun validation(ex: MethodArgumentNotValidException, req: HttpServletRequest): ResponseEntity<ApiError> {
        val fields = ex.bindingResult.fieldErrors.map {
            ApiError.FieldError(it.field, it.defaultMessage ?: "Invalid value")
        }
        return build(HttpStatus.BAD_REQUEST, "Validation failed", req, fields)
    }

    @ExceptionHandler(FieldValidationException::class)
    fun fieldValidation(ex: FieldValidationException, req: HttpServletRequest) =
        build(
            HttpStatus.BAD_REQUEST, "Validation failed", req,
            listOf(ApiError.FieldError(ex.field, ex.message ?: "Invalid value")),
        )

    @ExceptionHandler(BadRequestException::class)
    fun badRequest(ex: BadRequestException, req: HttpServletRequest) =
        build(HttpStatus.BAD_REQUEST, ex.message ?: "Bad request", req)

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun unreadable(ex: HttpMessageNotReadableException, req: HttpServletRequest) =
        build(HttpStatus.BAD_REQUEST, "Malformed request body or invalid value", req)

    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    fun typeMismatch(ex: MethodArgumentTypeMismatchException, req: HttpServletRequest) =
        build(HttpStatus.BAD_REQUEST, "Invalid value for parameter '${ex.name}'", req)

    @ExceptionHandler(NotFoundException::class)
    fun notFound(ex: NotFoundException, req: HttpServletRequest) =
        build(HttpStatus.NOT_FOUND, ex.message ?: "Not found", req)

    @ExceptionHandler(NoResourceFoundException::class)
    fun noRoute(ex: NoResourceFoundException, req: HttpServletRequest) =
        build(HttpStatus.NOT_FOUND, "Endpoint not found", req)

    @ExceptionHandler(ConflictException::class)
    fun conflict(ex: ConflictException, req: HttpServletRequest) =
        build(HttpStatus.CONFLICT, ex.message ?: "Conflict", req)

    @ExceptionHandler(DataIntegrityViolationException::class)
    fun integrity(ex: DataIntegrityViolationException, req: HttpServletRequest) =
        build(HttpStatus.CONFLICT, "Resource conflict", req)

    @ExceptionHandler(UnauthorizedException::class)
    fun unauthorized(ex: UnauthorizedException, req: HttpServletRequest) =
        build(HttpStatus.UNAUTHORIZED, ex.message ?: "Unauthorized", req)

    @ExceptionHandler(Exception::class)
    fun unexpected(ex: Exception, req: HttpServletRequest): ResponseEntity<ApiError> {
        log.error("Unhandled error on {} {}", req.method, req.requestURI, ex)
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error", req)
    }

    private fun build(
        status: HttpStatus,
        message: String,
        req: HttpServletRequest,
        fields: List<ApiError.FieldError> = emptyList(),
    ): ResponseEntity<ApiError> =
        ResponseEntity.status(status).body(
            ApiError(clock.instant(), status.value(), status.reasonPhrase, message, req.requestURI, fields)
        )
}
