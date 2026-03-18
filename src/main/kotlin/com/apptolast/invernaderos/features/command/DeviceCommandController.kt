package com.apptolast.invernaderos.features.command

import com.apptolast.invernaderos.features.telemetry.timescaledb.entities.DeviceCommand
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.Instant

/**
 * REST Controller para enviar comandos/consignas al PLC via MQTT.
 *
 * Endpoints:
 * - POST /api/v1/commands         — Enviar comando al PLC
 * - GET  /api/v1/commands/{code}  — Historial de comandos por code
 */
@RestController
@RequestMapping("/api/v1/commands")
@CrossOrigin(origins = ["*"])
class DeviceCommandController(
    private val deviceCommandService: DeviceCommandService
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * POST /api/v1/commands
     *
     * Envia un comando al PLC via MQTT y lo persiste en TimescaleDB.
     *
     * Request body: { "code": "SET-00036", "value": "22" }
     * Response: El comando persistido con timestamp de envio
     */
    @PostMapping
    fun sendCommand(@RequestBody request: SendCommandRequest): ResponseEntity<DeviceCommandResponse> {
        logger.info("POST /api/v1/commands - code={}, value={}", request.code, request.value)

        return try {
            val command = deviceCommandService.sendCommand(request.code, request.value)
            ResponseEntity.ok(command.toResponse())
        } catch (e: IllegalArgumentException) {
            logger.warn("Invalid command: {}", e.message)
            ResponseEntity.badRequest().build()
        } catch (e: Exception) {
            logger.error("Error sending command", e)
            ResponseEntity.internalServerError().build()
        }
    }

    /**
     * GET /api/v1/commands/{code}
     *
     * Historial de comandos enviados para un code.
     * Opcional: ?from=2026-03-01T00:00:00Z&to=2026-03-18T23:59:59Z
     */
    @GetMapping("/{code}")
    fun getCommandHistory(
        @PathVariable code: String,
        @RequestParam(required = false) from: Instant?,
        @RequestParam(required = false) to: Instant?
    ): ResponseEntity<List<DeviceCommandResponse>> {
        logger.debug("GET /api/v1/commands/{} from={} to={}", code, from, to)

        return try {
            val commands = if (from != null && to != null) {
                deviceCommandService.getCommandHistory(code, from, to)
            } else {
                deviceCommandService.getCommandHistory(code)
            }
            ResponseEntity.ok(commands.map { it.toResponse() })
        } catch (e: Exception) {
            logger.error("Error getting command history for code={}", code, e)
            ResponseEntity.internalServerError().build()
        }
    }
}

data class SendCommandRequest(
    val code: String,
    val value: String
)

data class DeviceCommandResponse(
    val time: Instant,
    val code: String,
    val value: String
)

fun DeviceCommand.toResponse() = DeviceCommandResponse(
    time = time,
    code = code,
    value = value
)
