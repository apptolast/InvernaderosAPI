package com.apptolast.invernaderos.features.command.dto.mapper

import com.apptolast.invernaderos.features.telemetry.timescaledb.entities.DeviceCommand as DeviceCommandEntity
import com.apptolast.invernaderos.features.command.domain.model.DeviceCommand
import com.apptolast.invernaderos.features.command.domain.port.input.SendDeviceCommand
import com.apptolast.invernaderos.features.command.dto.request.SendCommandRequest
import com.apptolast.invernaderos.features.command.dto.response.DeviceCommandResponse

// --- Request → Command ---

fun SendCommandRequest.toCommand() = SendDeviceCommand(
    code = code,
    value = value
)

// --- Entity ↔ Domain ---

fun DeviceCommandEntity.toDomain() = DeviceCommand(
    time = time,
    code = code,
    value = value
)

fun DeviceCommand.toEntity() = DeviceCommandEntity(
    time = time,
    code = code,
    value = value
)

// --- Domain → Response ---

fun DeviceCommand.toResponse() = DeviceCommandResponse(
    time = time,
    code = code,
    value = value
)
