package com.apptolast.invernaderos.features.device.domain.port.input

import com.apptolast.invernaderos.features.device.domain.model.CommandHistory
import com.apptolast.invernaderos.features.shared.domain.model.DeviceId

interface FindCommandHistoryUseCase {
    fun findByDeviceId(deviceId: DeviceId): List<CommandHistory>
    fun findRecentByDevice(deviceId: DeviceId, limit: Int): List<CommandHistory>
}
