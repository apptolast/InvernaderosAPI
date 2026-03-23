package com.apptolast.invernaderos.features.command.infrastructure.adapter.input

import com.apptolast.invernaderos.features.command.domain.port.input.QueryCommandHistoryUseCase
import com.apptolast.invernaderos.features.command.domain.port.input.SendCommandUseCase
import com.apptolast.invernaderos.features.command.dto.mapper.toCommand
import com.apptolast.invernaderos.features.command.dto.mapper.toResponse
import com.apptolast.invernaderos.features.command.dto.request.SendCommandRequest
import com.apptolast.invernaderos.features.command.dto.response.DeviceCommandResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.Instant

@RestController
@RequestMapping("/api/v1/commands")
@CrossOrigin(origins = ["*"])
@Tag(name = "Device Commands", description = "Endpoints para enviar comandos al PLC via MQTT")
class DeviceCommandController(
    private val sendCommandUseCase: SendCommandUseCase,
    private val queryHistoryUseCase: QueryCommandHistoryUseCase
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @PostMapping
    @Operation(summary = "Enviar un comando al PLC via MQTT")
    fun sendCommand(@RequestBody request: SendCommandRequest): ResponseEntity<Any> {
        logger.info("POST /api/v1/commands - code={}, value={}", request.code, request.value)

        return sendCommandUseCase.execute(request.toCommand()).fold(
            onLeft = { error ->
                logger.warn("Invalid command: {}", error.message)
                ResponseEntity.status(HttpStatus.BAD_REQUEST).body(mapOf("error" to error.message))
            },
            onRight = { command ->
                ResponseEntity.ok(command.toResponse())
            }
        )
    }

    @GetMapping("/{code}")
    @Operation(summary = "Historial de comandos por código")
    fun getCommandHistory(
        @PathVariable code: String,
        @RequestParam(required = false) from: Instant?,
        @RequestParam(required = false) to: Instant?
    ): ResponseEntity<List<DeviceCommandResponse>> {
        logger.debug("GET /api/v1/commands/{} from={} to={}", code, from, to)

        val commands = if (from != null && to != null) {
            queryHistoryUseCase.getHistory(code, from, to)
        } else {
            queryHistoryUseCase.getHistory(code)
        }
        return ResponseEntity.ok(commands.map { it.toResponse() })
    }
}
