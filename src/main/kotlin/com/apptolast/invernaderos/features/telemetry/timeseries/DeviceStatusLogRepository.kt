package com.apptolast.invernaderos.features.telemetry.timeseries

import com.apptolast.invernaderos.features.telemetry.timescaledb.entities.DeviceStatusLog
import com.apptolast.invernaderos.features.telemetry.timescaledb.entities.DeviceStatusLogId
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.Instant

@Repository
interface DeviceStatusLogRepository : JpaRepository<DeviceStatusLog, DeviceStatusLogId> {

    /**
     * Obtiene el historial de un código específico ordenado por tiempo descendente
     */
    @Query("SELECT d FROM DeviceStatusLog d WHERE d.code = :code ORDER BY d.time DESC")
    fun findByCodeOrderByTimeDesc(@Param("code") code: String): List<DeviceStatusLog>

    /**
     * Obtiene el historial de un código en un rango de tiempo
     */
    @Query("SELECT d FROM DeviceStatusLog d WHERE d.code = :code AND d.time BETWEEN :start AND :end ORDER BY d.time DESC")
    fun findByCodeAndTimeBetween(
        @Param("code") code: String,
        @Param("start") start: Instant,
        @Param("end") end: Instant
    ): List<DeviceStatusLog>

    /**
     * Obtiene el último valor registrado para un código
     */
    @Query(value = """
        SELECT * FROM iot.device_status_log
        WHERE code = :code
        ORDER BY time DESC
        LIMIT 1
    """, nativeQuery = true)
    fun findLatestByCode(@Param("code") code: String): DeviceStatusLog?

    /**
     * Obtiene el último valor registrado para cada código (snapshot actual)
     */
    @Query(value = """
        SELECT DISTINCT ON (code) *
        FROM iot.device_status_log
        ORDER BY code, time DESC
    """, nativeQuery = true)
    fun findLatestForAllCodes(): List<DeviceStatusLog>
}
