package com.apptolast.invernaderos.features.telemetry.timeseries

import com.apptolast.invernaderos.features.telemetry.timescaledb.entities.SensorReadingId
import com.apptolast.invernaderos.features.telemetry.timescaledb.entities.SensorReadingRaw
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

/**
 * Repository para el archivo fiel de lecturas (sin deduplicacion).
 * Solo necesita saveAll() para batch insert.
 */
@Repository
interface SensorReadingRawRepository : JpaRepository<SensorReadingRaw, SensorReadingId>
