package com.apptolast.invernaderos.features.command.domain.port.output

import com.apptolast.invernaderos.features.command.domain.model.DeviceCommand
import java.time.Instant

interface DeviceCommandPersistencePort {
    fun save(command: DeviceCommand): DeviceCommand
    fun findByCodeOrderByTimeDesc(code: String): List<DeviceCommand>
    fun findByCodeAndTimeBetween(code: String, from: Instant, to: Instant): List<DeviceCommand>
    fun findLatestByCode(code: String): DeviceCommand?
}
