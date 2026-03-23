package com.apptolast.invernaderos.features.command.domain.model

import java.time.Instant

data class DeviceCommand(
    val time: Instant,
    val code: String,
    val value: String
)
