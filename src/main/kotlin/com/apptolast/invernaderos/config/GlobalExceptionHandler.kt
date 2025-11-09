package com.apptolast.invernaderos.config

import com.fasterxml.jackson.databind.exc.InvalidDefinitionException
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import org.springframework.web.servlet.NoHandlerFoundException
import java.time.Instant

/**
 * Global exception handler usando Spring Boot 3+ RFC 7807 (ProblemDetail)
 *
 * Centraliza el manejo de excepciones para toda la aplicación REST
 * Basado en: https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-ann-rest-exceptions.html
 */
@RestControllerAdvice
class GlobalExceptionHandler {

    private val logger = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    /**
     * Maneja excepciones generales no capturadas
     */
    @ExceptionHandler(Exception::class)
    fun handleGeneralException(ex: Exception): ResponseEntity<ProblemDetail> {
        logger.error("Unhandled exception: {}", ex.message, ex)

        val problemDetail = ProblemDetail.forStatusAndDetail(
            HttpStatus.INTERNAL_SERVER_ERROR,
            ex.message ?: "An unexpected error occurred"
        )
        problemDetail.title = "Internal Server Error"
        problemDetail.setProperty("timestamp", Instant.now())

        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(problemDetail)
    }

    /**
     * Maneja excepciones de validación (@Valid en request bodies)
     */
    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationException(ex: MethodArgumentNotValidException): ResponseEntity<ProblemDetail> {
        logger.warn("Validation error: {}", ex.message)

        val errors = ex.bindingResult.fieldErrors.associate {
            it.field to (it.defaultMessage ?: "Invalid value")
        }

        val problemDetail = ProblemDetail.forStatusAndDetail(
            HttpStatus.BAD_REQUEST,
            "Validation failed for one or more fields"
        )
        problemDetail.title = "Bad Request"
        problemDetail.setProperty("timestamp", Instant.now())
        problemDetail.setProperty("errors", errors)

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(problemDetail)
    }

    /**
     * Maneja errores de tipo en path variables o request params
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    fun handleTypeMismatch(ex: MethodArgumentTypeMismatchException): ResponseEntity<ProblemDetail> {
        logger.warn("Type mismatch error: {}", ex.message)

        val problemDetail = ProblemDetail.forStatusAndDetail(
            HttpStatus.BAD_REQUEST,
            "Invalid parameter type: '${ex.name}' should be of type ${ex.requiredType?.simpleName}"
        )
        problemDetail.title = "Bad Request"
        problemDetail.setProperty("timestamp", Instant.now())
        problemDetail.setProperty("parameter", ex.name)

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(problemDetail)
    }

    /**
     * Maneja recursos no encontrados (404)
     */
    @ExceptionHandler(NoHandlerFoundException::class)
    fun handleNotFound(ex: NoHandlerFoundException): ResponseEntity<ProblemDetail> {
        logger.warn("Endpoint not found: {} {}", ex.httpMethod, ex.requestURL)

        val problemDetail = ProblemDetail.forStatusAndDetail(
            HttpStatus.NOT_FOUND,
            "The requested endpoint does not exist"
        )
        problemDetail.title = "Not Found"
        problemDetail.setProperty("timestamp", Instant.now())
        problemDetail.setProperty("path", ex.requestURL)
        problemDetail.setProperty("method", ex.httpMethod)

        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(problemDetail)
    }

    /**
     * Maneja errores de serialización de Jackson (ej: Instant sin JavaTimeModule)
     */
    @ExceptionHandler(InvalidDefinitionException::class)
    fun handleJacksonInvalidDefinition(ex: InvalidDefinitionException): ResponseEntity<ProblemDetail> {
        logger.error("Jackson serialization error: {}", ex.message, ex)

        val problemDetail = ProblemDetail.forStatusAndDetail(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "Error serializing response. This might be due to missing Jackson modules."
        )
        problemDetail.title = "Serialization Error"
        problemDetail.setProperty("timestamp", Instant.now())
        problemDetail.setProperty("type", ex.type?.rawClass?.simpleName)

        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(problemDetail)
    }

    /**
     * Maneja IllegalArgumentException (usualmente de validaciones de negocio)
     */
    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgument(ex: IllegalArgumentException): ResponseEntity<ProblemDetail> {
        logger.warn("Illegal argument: {}", ex.message)

        val problemDetail = ProblemDetail.forStatusAndDetail(
            HttpStatus.BAD_REQUEST,
            ex.message ?: "Invalid argument provided"
        )
        problemDetail.title = "Bad Request"
        problemDetail.setProperty("timestamp", Instant.now())

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(problemDetail)
    }

    /**
     * Maneja IllegalStateException (errores de estado de la aplicación)
     */
    @ExceptionHandler(IllegalStateException::class)
    fun handleIllegalState(ex: IllegalStateException): ResponseEntity<ProblemDetail> {
        logger.error("Illegal state: {}", ex.message, ex)

        val problemDetail = ProblemDetail.forStatusAndDetail(
            HttpStatus.CONFLICT,
            ex.message ?: "The operation cannot be performed in the current state"
        )
        problemDetail.title = "Conflict"
        problemDetail.setProperty("timestamp", Instant.now())

        return ResponseEntity
            .status(HttpStatus.CONFLICT)
            .body(problemDetail)
    }

    /**
     * Maneja NoSuchElementException (recursos no encontrados)
     */
    @ExceptionHandler(NoSuchElementException::class)
    fun handleNoSuchElement(ex: NoSuchElementException): ResponseEntity<ProblemDetail> {
        logger.warn("Resource not found: {}", ex.message)

        val problemDetail = ProblemDetail.forStatusAndDetail(
            HttpStatus.NOT_FOUND,
            ex.message ?: "The requested resource was not found"
        )
        problemDetail.title = "Not Found"
        problemDetail.setProperty("timestamp", Instant.now())

        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(problemDetail)
    }
}
