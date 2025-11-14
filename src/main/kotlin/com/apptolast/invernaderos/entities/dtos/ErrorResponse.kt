package com.apptolast.invernaderos.entities.dtos

/**
 * Respuesta de error estándar para la API
 *
 * Utilizada en respuestas HTTP de error para proporcionar información
 * estructurada sobre el error ocurrido.
 */
data class ErrorResponse(
    val error: String,
    val message: String,
    val timestamp: String = java.time.Instant.now().toString()
)
