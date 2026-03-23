package com.apptolast.invernaderos.features.device.domain.port.output

import com.apptolast.invernaderos.features.device.domain.model.CommandHistory
import com.apptolast.invernaderos.features.shared.domain.model.DeviceId

interface CommandHistoryRepositoryPort {
    fun findByDeviceId(deviceId: DeviceId): List<CommandHistory>
    fun findRecentByDevice(deviceId: DeviceId, limit: Int): List<CommandHistory>
}
