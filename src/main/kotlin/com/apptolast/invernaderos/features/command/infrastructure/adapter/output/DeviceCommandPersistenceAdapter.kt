package com.apptolast.invernaderos.features.command.infrastructure.adapter.output

import com.apptolast.invernaderos.features.command.domain.model.DeviceCommand
import com.apptolast.invernaderos.features.command.domain.port.output.DeviceCommandPersistencePort
import com.apptolast.invernaderos.features.command.dto.mapper.toDomain
import com.apptolast.invernaderos.features.command.dto.mapper.toEntity
import com.apptolast.invernaderos.features.telemetry.timeseries.DeviceCommandRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Component
class DeviceCommandPersistenceAdapter(
    private val repository: DeviceCommandRepository
) : DeviceCommandPersistencePort {

    @Transactional("timescaleTransactionManager")
    override fun save(command: DeviceCommand): DeviceCommand {
        return repository.save(command.toEntity()).toDomain()
    }

    override fun findByCodeOrderByTimeDesc(code: String): List<DeviceCommand> {
        return repository.findByCodeOrderByTimeDesc(code).map { it.toDomain() }
    }

    override fun findByCodeAndTimeBetween(code: String, from: Instant, to: Instant): List<DeviceCommand> {
        return repository.findByCodeAndTimeBetween(code, from, to).map { it.toDomain() }
    }

    override fun findLatestByCode(code: String): DeviceCommand? {
        return repository.findLatestByCode(code)?.toDomain()
    }
}
