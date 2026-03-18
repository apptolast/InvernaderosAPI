package com.apptolast.invernaderos.features.telemetry.timeseries

import com.apptolast.invernaderos.features.telemetry.timescaledb.entities.DeviceCommand
import com.apptolast.invernaderos.features.telemetry.timescaledb.entities.DeviceCommandId
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.Instant

@Repository
interface DeviceCommandRepository : JpaRepository<DeviceCommand, DeviceCommandId> {

    /**
     * Historial de comandos para un code, ordenado por tiempo descendente
     */
    @Query("SELECT dc FROM DeviceCommand dc WHERE dc.code = :code ORDER BY dc.time DESC")
    fun findByCodeOrderByTimeDesc(@Param("code") code: String): List<DeviceCommand>

    /**
     * Comandos en un rango de tiempo para un code
     */
    @Query("SELECT dc FROM DeviceCommand dc WHERE dc.code = :code AND dc.time BETWEEN :start AND :end ORDER BY dc.time DESC")
    fun findByCodeAndTimeBetween(
        @Param("code") code: String,
        @Param("start") start: Instant,
        @Param("end") end: Instant
    ): List<DeviceCommand>

    /**
     * Ultimo comando enviado para un code
     */
    @Query(value = """
        SELECT * FROM iot.device_commands
        WHERE code = :code
        ORDER BY time DESC
        LIMIT 1
    """, nativeQuery = true)
    fun findLatestByCode(@Param("code") code: String): DeviceCommand?
}
