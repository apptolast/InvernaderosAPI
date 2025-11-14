package com.apptolast.invernaderos.controllers

import com.apptolast.invernaderos.config.SimulationProperties
import com.apptolast.invernaderos.entities.dtos.RealDataDto
import com.apptolast.invernaderos.entities.dtos.toJson
import com.apptolast.invernaderos.mqtt.service.MqttMessageProcessor
import com.apptolast.invernaderos.service.GreenhouseDataSimulator
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.constraints.Pattern
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*

/**
 * Controlador REST para gestión de simulación de datos de invernadero
 *
 * Proporciona endpoints para:
 * - Generar datos simulados bajo demanda (testing/debugging)
 * - Ver estado de la simulación
 * - Información sobre la configuración activa
 *
 * Solo disponible cuando la simulación está habilitada en configuración
 *
 * Base URL: /api/simulation
 */
@RestController
@RequestMapping("/api/simulation")
@Tag(name = "Simulation", description = "Control y gestión de simulación de datos de invernadero")
@ConditionalOnProperty(
    prefix = "greenhouse.simulation",
    name = ["enabled"],
    havingValue = "true",
    matchIfMissing = false
)
@Validated
class SimulationController(
    private val dataSimulator: GreenhouseDataSimulator,
    private val messageProcessor: MqttMessageProcessor,
    private val properties: SimulationProperties
) {

    private val logger = LoggerFactory.getLogger(SimulationController::class.java)

    /**
     * Genera y procesa un conjunto de datos simulados bajo demanda
     *
     * Útil para:
     * - Testing de la aplicación móvil sin esperar al scheduler
     * - Debugging del flujo de datos
     * - Verificar integración con WebSocket
     *
     * @param greenhouseId ID del invernadero (opcional, default: "001")
     * @return RealDataDto generado
     */
    @PostMapping("/generate")
    @Operation(
        summary = "Generar datos simulados bajo demanda",
        description = "Genera un conjunto de datos simulados y los procesa inmediatamente (Redis + DB + WebSocket)"
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Datos generados y procesados exitosamente",
                content = [Content(schema = Schema(implementation = RealDataDto::class))]
            ),
            ApiResponse(
                responseCode = "500",
                description = "Error generando o procesando datos",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            )
        ]
    )
    fun generateSimulatedData(
        @Parameter(description = "ID del invernadero a simular", example = "001")
        @RequestParam(defaultValue = "001")
        @Pattern(regexp = "^[0-9]{3}$", message = "Greenhouse ID must be a 3-digit number")
        greenhouseId: String
    ): ResponseEntity<Any> {
        return try {
            logger.info("Generando datos simulados bajo demanda para greenhouse: {}", greenhouseId)

            // 1. Generar datos
            val simulatedData = dataSimulator.generateRealisticData(greenhouseId)

            // 2. Convertir a JSON
            val jsonPayload = simulatedData.toJson()

            // 3. Procesar (Redis + DB + WebSocket)
            messageProcessor.processGreenhouseData(jsonPayload, greenhouseId)

            logger.info("Datos simulados generados y procesados exitosamente")

            ResponseEntity.ok(simulatedData)

        } catch (e: Exception) {
            logger.error("Error generando datos simulados: ${e.message}", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse(
                    error = "SIMULATION_ERROR",
                    message = "Error generating simulated data: ${e.message ?: "Unknown error"}"
                ))
        }
    }

    /**
     * Obtiene el estado actual de la simulación
     *
     * @return Estado de la simulación con información de configuración
     */
    @GetMapping("/status")
    @Operation(
        summary = "Ver estado de la simulación",
        description = "Muestra si la simulación está activa y su configuración actual"
    )
    @ApiResponse(
        responseCode = "200",
        description = "Estado de la simulación",
        content = [Content(schema = Schema(implementation = SimulationStatus::class))]
    )
    fun getSimulationStatus(): ResponseEntity<SimulationStatus> {
        logger.debug("Consultando estado de simulación")

        val status = SimulationStatus(
            enabled = properties.enabled,
            schedulerActive = properties.enabled,
            intervalMs = properties.intervalMs,
            greenhouseId = properties.greenhouseId,
            message = "Simulación activa - Generando datos cada ${properties.intervalMs / 1000} segundos"
        )

        return ResponseEntity.ok(status)
    }

    /**
     * Genera solo datos sin procesarlos (para testing/preview)
     *
     * @param greenhouseId ID del invernadero
     * @return RealDataDto generado (sin guardar en BD ni enviar por WebSocket)
     */
    @GetMapping("/preview")
    @Operation(
        summary = "Preview de datos simulados",
        description = "Genera datos simulados SIN procesarlos (no se guardan en BD ni se envían por WebSocket)"
    )
    @ApiResponse(
        responseCode = "200",
        description = "Datos generados (solo preview)",
        content = [Content(schema = Schema(implementation = RealDataDto::class))]
    )
    fun previewSimulatedData(
        @Parameter(description = "ID del invernadero", example = "001")
        @RequestParam(defaultValue = "001")
        @Pattern(regexp = "^[0-9]{3}$", message = "Greenhouse ID must be a 3-digit number")
        greenhouseId: String
    ): ResponseEntity<RealDataDto> {
        logger.debug("Generando preview de datos simulados para greenhouse: {}", greenhouseId)

        val simulatedData = dataSimulator.generateRealisticData(greenhouseId)

        return ResponseEntity.ok(simulatedData)
    }

    /**
     * Información sobre la API de simulación
     */
    @GetMapping("/info")
    @Operation(
        summary = "Información de la API de simulación",
        description = "Muestra información sobre cómo usar la simulación"
    )
    @ApiResponse(
        responseCode = "200",
        description = "Información de la API",
        content = [Content(schema = Schema(implementation = SimulationInfo::class))]
    )
    fun getInfo(): ResponseEntity<SimulationInfo> {
        val intervalSeconds = properties.intervalMs / 1000
        val info = SimulationInfo(
            version = "1.0",
            description = "API para simular datos de invernadero cuando sensores físicos no están disponibles",
            endpoints = listOf(
                EndpointInfo(
                    method = "POST",
                    path = "/api/simulation/generate",
                    description = "Genera y procesa datos simulados inmediatamente",
                    parameters = listOf("greenhouseId (opcional)")
                ),
                EndpointInfo(
                    method = "GET",
                    path = "/api/simulation/status",
                    description = "Ver estado y configuración de la simulación",
                    parameters = emptyList()
                ),
                EndpointInfo(
                    method = "GET",
                    path = "/api/simulation/preview",
                    description = "Generar datos sin procesarlos (preview)",
                    parameters = listOf("greenhouseId (opcional)")
                ),
                EndpointInfo(
                    method = "GET",
                    path = "/api/simulation/info",
                    description = "Información sobre la API",
                    parameters = emptyList()
                )
            ),
            dataRanges = DataRanges(
                temperature = "15-35°C (distribución normal)",
                humidity = "30-90%",
                sectors = "0-100% (70% probabilidad 30-70%, 30% extremos)",
                extractors = "0.0 o 1.0 (30% probabilidad encendido)",
                reserva = "0-100"
            ),
            notes = listOf(
                "Los datos generados pasan por el mismo flujo que datos reales",
                "Se guardan en Redis (cache) y TimescaleDB (persistencia)",
                "Se publican vía WebSocket a clientes conectados",
                "El scheduler automático genera datos cada $intervalSeconds segundos",
                "Esta API solo está disponible cuando greenhouse.simulation.enabled=true"
            )
        )

        return ResponseEntity.ok(info)
    }
}

// ============================================================================
// DTOs para respuestas
// ============================================================================

/**
 * Estado de la simulación
 */
data class SimulationStatus(
    val enabled: Boolean,
    val schedulerActive: Boolean,
    val intervalMs: Int,
    val greenhouseId: String,
    val message: String
)

/**
 * Información sobre la API de simulación
 */
data class SimulationInfo(
    val version: String,
    val description: String,
    val endpoints: List<EndpointInfo>,
    val dataRanges: DataRanges,
    val notes: List<String>
)

/**
 * Información de un endpoint
 */
data class EndpointInfo(
    val method: String,
    val path: String,
    val description: String,
    val parameters: List<String>
)

/**
 * Rangos de datos generados
 */
data class DataRanges(
    val temperature: String,
    val humidity: String,
    val sectors: String,
    val extractors: String,
    val reserva: String
)

/**
 * Respuesta de error
 */
data class ErrorResponse(
    val error: String,
    val message: String,
    val timestamp: String = java.time.Instant.now().toString()
)
