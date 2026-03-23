package com.apptolast.invernaderos.features.device.application.usecase

import com.apptolast.invernaderos.features.device.domain.model.CommandHistory
import com.apptolast.invernaderos.features.device.domain.port.input.FindCommandHistoryUseCase
import com.apptolast.invernaderos.features.device.domain.port.output.CommandHistoryRepositoryPort
import com.apptolast.invernaderos.features.shared.domain.model.DeviceId

class FindCommandHistoryUseCaseImpl(
    private val repository: CommandHistoryRepositoryPort
) : FindCommandHistoryUseCase {

    override fun findByDeviceId(deviceId: DeviceId): List<CommandHistory> {
        return repository.findByDeviceId(deviceId)
    }

    override fun findRecentByDevice(deviceId: DeviceId, limit: Int): List<CommandHistory> {
        return repository.findRecentByDevice(deviceId, limit)
    }
}
