package com.apptolast.invernaderos.features.telemetry.timeseries

import com.apptolast.invernaderos.features.telemetry.timescaledb.entities.DeviceCurrentValue
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface DeviceCurrentValueRepository : JpaRepository<DeviceCurrentValue, String> {

    /**
     * UPSERT: inserta o actualiza el valor actual de un code.
     * Si el code ya existe, actualiza value, last_seen_at e incrementa update_count.
     * Si no existe, inserta con first_seen_at = NOW().
     */
    @Modifying
    @Query(value = """
        INSERT INTO iot.device_current_values (code, value, first_seen_at, last_seen_at, update_count)
        VALUES (:code, :value, :timestamp, :timestamp, 1)
        ON CONFLICT (code) DO UPDATE SET
            value = :value,
            last_seen_at = :timestamp,
            update_count = iot.device_current_values.update_count + 1
    """, nativeQuery = true)
    fun upsert(
        @Param("code") code: String,
        @Param("value") value: String,
        @Param("timestamp") timestamp: java.time.Instant
    )

    /**
     * Busca por code individual
     */
    fun findByCode(code: String): DeviceCurrentValue?
}
