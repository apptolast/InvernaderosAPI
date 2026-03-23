package com.apptolast.invernaderos.features.sensor.domain.port.input

import com.apptolast.invernaderos.features.sensor.domain.model.SensorReading
import java.time.Instant

interface QuerySensorReadingsUseCase {
    fun getLatestReadings(limit: Int): List<SensorReading>
    fun getReadingsByCode(code: String, start: Instant, end: Instant): List<SensorReading>
    fun getCurrentValues(): Map<String, Any?>
}
