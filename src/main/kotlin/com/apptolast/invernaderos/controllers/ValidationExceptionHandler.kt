package com.apptolast.invernaderos.controllers

import com.apptolast.invernaderos.entities.dtos.ErrorResponse
import jakarta.validation.ConstraintViolationException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler

/**
 * Manejador global de excepciones de validación
 *
 * Captura ConstraintViolationException lanzadas por validaciones de Jakarta Bean Validation
 * y las convierte en respuestas HTTP 400 Bad Request con mensajes descriptivos.
 */
@ControllerAdvice
class ValidationExceptionHandler {

    /**
     * Maneja excepciones de violación de restricciones de validación
     *
     * @param ex La excepción de violación de restricciones
     * @return ResponseEntity con código 400 y detalles del error
     */
    @ExceptionHandler(ConstraintViolationException::class)
    fun handleConstraintViolation(ex: ConstraintViolationException): ResponseEntity<ErrorResponse> {
        val message = ex.constraintViolations.joinToString(", ") { it.message }
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ErrorResponse(
                error = "VALIDATION_ERROR",
                message = message
            ))
    }
}
