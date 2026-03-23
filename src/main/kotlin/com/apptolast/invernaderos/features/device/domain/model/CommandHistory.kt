package com.apptolast.invernaderos.features.device.domain.model

import com.apptolast.invernaderos.features.shared.domain.model.DeviceId
import java.time.Instant

data class CommandHistory(
    val id: Long?,
    val code: String,
    val deviceId: DeviceId,
    val command: String,
    val value: Double?,
    val source: String?,
    val userId: Long?,
    val success: Boolean?,
    val response: String?,
    val createdAt: Instant
)
