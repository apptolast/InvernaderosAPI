package com.apptolast.invernaderos.mqtt.controller

import com.apptolast.invernaderos.mqtt.service.RateLimiterStats
import com.apptolast.invernaderos.mqtt.service.SensorRateLimiter
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Controller para monitorear y gestionar el rate limiter de sensores.
 *
 * Endpoints:
 * - GET /api/v1/rate-limiter/stats - Obtener estadísticas actuales
 * - POST /api/v1/rate-limiter/reset-stats - Resetear contadores de estadísticas
 */
@RestController
@RequestMapping("/api/v1/rate-limiter")
@Tag(name = "Rate Limiter", description = "Gestión del rate limiter de lecturas de sensores")
class RateLimiterController(
    private val sensorRateLimiter: SensorRateLimiter
) {

    @GetMapping("/stats")
    @Operation(
        summary = "Obtener estadísticas del rate limiter",
        description = """
            Retorna estadísticas sobre el funcionamiento del rate limiter:
            - enabled: Si está activo
            - minIntervalSeconds: Intervalo mínimo entre lecturas
            - totalReceived: Total de lecturas recibidas
            - totalSaved: Lecturas guardadas en TimescaleDB
            - totalDropped: Lecturas descartadas por rate limiting
            - dropRatePercent: Porcentaje de lecturas descartadas
        """
    )
    fun getStats(): ResponseEntity<RateLimiterStats> {
        return ResponseEntity.ok(sensorRateLimiter.getStats())
    }

    @PostMapping("/reset-stats")
    @Operation(
        summary = "Resetear estadísticas",
        description = "Pone a cero los contadores de estadísticas"
    )
    fun resetStats(): ResponseEntity<Map<String, String>> {
        sensorRateLimiter.resetStats()
        return ResponseEntity.ok(mapOf(
            "status" to "ok",
            "message" to "Estadísticas reseteadas"
        ))
    }
}
