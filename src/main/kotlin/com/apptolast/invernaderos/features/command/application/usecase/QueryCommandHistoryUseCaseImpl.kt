package com.apptolast.invernaderos.features.command.application.usecase

import com.apptolast.invernaderos.features.command.domain.model.DeviceCommand
import com.apptolast.invernaderos.features.command.domain.port.input.QueryCommandHistoryUseCase
import com.apptolast.invernaderos.features.command.domain.port.output.DeviceCommandPersistencePort
import java.time.Instant

class QueryCommandHistoryUseCaseImpl(
    private val persistence: DeviceCommandPersistencePort
) : QueryCommandHistoryUseCase {

    override fun getHistory(code: String): List<DeviceCommand> {
        return persistence.findByCodeOrderByTimeDesc(code)
    }

    override fun getHistory(code: String, from: Instant, to: Instant): List<DeviceCommand> {
        return persistence.findByCodeAndTimeBetween(code, from, to)
    }

    override fun getLatest(code: String): DeviceCommand? {
        return persistence.findLatestByCode(code)
    }
}
