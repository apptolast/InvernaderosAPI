package com.apptolast.invernaderos.features.sensor.domain.port.output

import com.apptolast.invernaderos.features.sensor.domain.model.SensorReading
import java.time.Instant

interface SensorReadingQueryPort {
    fun findTopNOrderByTimeDesc(limit: Int): List<SensorReading>
    fun findByCodeAndTimeBetween(code: String, start: Instant, end: Instant): List<SensorReading>
    fun findLatestForAllCodes(): List<SensorReading>
}
