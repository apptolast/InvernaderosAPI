package com.apptolast.invernaderos.features.device.dto.mapper

import com.apptolast.invernaderos.features.device.CommandHistory as CommandHistoryEntity
import com.apptolast.invernaderos.features.device.domain.model.CommandHistory
import com.apptolast.invernaderos.features.device.dto.response.CommandHistoryResponse
import com.apptolast.invernaderos.features.shared.domain.model.DeviceId

// --- Entity → Domain ---

fun CommandHistoryEntity.toDomain() = CommandHistory(
    id = id,
    code = code,
    deviceId = DeviceId(deviceId),
    command = command,
    value = value,
    source = source,
    userId = userId,
    success = success,
    response = response,
    createdAt = createdAt
)

// --- Domain → Response ---

fun CommandHistory.toResponse() = CommandHistoryResponse(
    id = id ?: throw IllegalStateException("CommandHistory ID cannot be null"),
    code = code,
    deviceId = deviceId.value,
    command = command,
    value = value,
    source = source,
    userId = userId,
    success = success,
    response = response,
    createdAt = createdAt
)
