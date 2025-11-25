package com.apptolast.invernaderos.features.actuator

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import java.time.Instant
import java.util.UUID

@Schema(description = "Response object representing an Actuator")
data class ActuatorResponse(
        val id: UUID,
        val tenantId: UUID,
        val greenhouseId: UUID,
        val actuatorCode: String,
        val deviceId: String,
        val actuatorType: String,
        val currentState: String?,
        val currentValue: Double?,
        val unit: String?,
        val isActive: Boolean,
        val lastCommandAt: Instant?,
        val lastStatusUpdate: Instant?
)

@Schema(description = "Request object to create a new Actuator")
data class ActuatorCreateRequest(
        @field:NotBlank(message = "Greenhouse ID is required") val greenhouseId: UUID,
        @field:NotBlank(message = "Actuator Code is required") val actuatorCode: String,
        @field:NotBlank(message = "Device ID is required") val deviceId: String,
        @field:NotBlank(message = "Actuator Type is required") val actuatorType: String,
        val unit: String? = null,
        val locationInGreenhouse: String? = null
)

@Schema(description = "Request object to send a command to an Actuator")
data class ActuatorCommandRequest(
        @Schema(description = "Command action (ON, OFF, AUTO, MANUAL, SET_VALUE)", example = "ON")
        @field:NotBlank(message = "Command is required")
        val command: String,
        @Schema(description = "Value for SET_VALUE command", example = "50.0")
        val value: Double? = null
)

fun Actuator.toResponse() =
        ActuatorResponse(
                id = this.id!!,
                tenantId = this.tenantId,
                greenhouseId = this.greenhouseId,
                actuatorCode = this.actuatorCode,
                deviceId = this.deviceId,
                actuatorType = this.actuatorType,
                currentState = this.currentState,
                currentValue = this.currentValue,
                unit = this.unit,
                isActive = this.isActive,
                lastCommandAt = this.lastCommandAt,
                lastStatusUpdate = this.lastStatusUpdate
        )
