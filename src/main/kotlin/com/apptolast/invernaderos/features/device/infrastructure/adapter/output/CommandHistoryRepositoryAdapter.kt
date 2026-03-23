package com.apptolast.invernaderos.features.device.infrastructure.adapter.output

import com.apptolast.invernaderos.features.device.CommandHistoryRepository
import com.apptolast.invernaderos.features.device.domain.model.CommandHistory
import com.apptolast.invernaderos.features.device.domain.port.output.CommandHistoryRepositoryPort
import com.apptolast.invernaderos.features.device.dto.mapper.toDomain
import com.apptolast.invernaderos.features.shared.domain.model.DeviceId
import org.springframework.stereotype.Component

@Component
class CommandHistoryRepositoryAdapter(
    private val jpaRepository: CommandHistoryRepository
) : CommandHistoryRepositoryPort {

    override fun findByDeviceId(deviceId: DeviceId): List<CommandHistory> {
        return jpaRepository.findByDeviceIdOrderByCreatedAtDesc(deviceId.value).map { it.toDomain() }
    }

    override fun findRecentByDevice(deviceId: DeviceId, limit: Int): List<CommandHistory> {
        return jpaRepository.findRecentByDevice(deviceId.value, limit).map { it.toDomain() }
    }
}
