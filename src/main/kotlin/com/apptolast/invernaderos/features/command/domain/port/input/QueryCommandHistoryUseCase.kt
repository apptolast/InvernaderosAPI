package com.apptolast.invernaderos.features.command.domain.port.input

import com.apptolast.invernaderos.features.command.domain.model.DeviceCommand
import java.time.Instant

interface QueryCommandHistoryUseCase {
    fun getHistory(code: String): List<DeviceCommand>
    fun getHistory(code: String, from: Instant, to: Instant): List<DeviceCommand>
    fun getLatest(code: String): DeviceCommand?
}
