package com.apptolast.invernaderos.features.sensor.infrastructure.adapter.output

import com.apptolast.invernaderos.features.sensor.domain.model.SensorReading
import com.apptolast.invernaderos.features.sensor.domain.port.output.SensorReadingQueryPort
import com.apptolast.invernaderos.features.sensor.dto.mapper.toDomain
import com.apptolast.invernaderos.features.telemetry.timeseries.SensorReadingRepository
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class SensorReadingQueryAdapter(
    private val repository: SensorReadingRepository
) : SensorReadingQueryPort {

    override fun findTopNOrderByTimeDesc(limit: Int): List<SensorReading> {
        return repository.findTopNOrderByTimeDesc(limit).map { it.toDomain() }
    }

    override fun findByCodeAndTimeBetween(code: String, start: Instant, end: Instant): List<SensorReading> {
        return repository.findByCodeAndTimeBetween(code, start, end).map { it.toDomain() }
    }

    override fun findLatestForAllCodes(): List<SensorReading> {
        return repository.findLatestForAllCodes().map { it.toDomain() }
    }
}
