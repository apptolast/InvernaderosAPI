package com.apptolast.invernaderos.features.actuator

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import jakarta.validation.Valid
import java.util.UUID
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/actuators")
class ActuatorController(private val actuatorService: ActuatorService) {

    @GetMapping
    @Operation(summary = "List all actuators")
    fun getAllActuators(): ResponseEntity<List<ActuatorResponse>> {
        return ResponseEntity.ok(actuatorService.findAll())
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get actuator details")
    @ApiResponse(responseCode = "200", description = "Actuator found")
    @ApiResponse(responseCode = "404", description = "Actuator not found")
    fun getActuator(@PathVariable id: UUID): ResponseEntity<ActuatorResponse> {
        val actuator = actuatorService.findById(id)
        return if (actuator != null) {
            ResponseEntity.ok(actuator)
        } else {
            ResponseEntity.notFound().build()
        }
    }

    @PostMapping
    @Operation(summary = "Create a new actuator")
    @ApiResponse(responseCode = "200", description = "Actuator created")
    fun createActuator(
            @Valid @RequestBody request: ActuatorCreateRequest
    ): ResponseEntity<ActuatorResponse> {
        // Tenant ID should be extracted from Security Context, hardcoded for now or passed
        // Assuming a default tenant or extracted from context in real app
        val tenantId = UUID.fromString("00000000-0000-0000-0000-000000000000") // Placeholder
        return ResponseEntity.ok(actuatorService.create(request, tenantId))
    }

    @PostMapping("/{id}/command")
    @Operation(summary = "Send command to actuator")
    @ApiResponse(responseCode = "200", description = "Command sent")
    @ApiResponse(responseCode = "404", description = "Actuator not found")
    fun sendCommand(
            @PathVariable id: UUID,
            @Valid @RequestBody request: ActuatorCommandRequest
    ): ResponseEntity<ActuatorResponse> {
        val updatedActuator = actuatorService.sendCommand(id, request)
        return if (updatedActuator != null) {
            ResponseEntity.ok(updatedActuator)
        } else {
            ResponseEntity.notFound().build()
        }
    }
}
