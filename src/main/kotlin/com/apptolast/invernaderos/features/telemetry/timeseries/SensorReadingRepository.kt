package com.apptolast.invernaderos.features.telemetry.timeseries

import com.apptolast.invernaderos.features.telemetry.timescaledb.entities.SensorReading
import com.apptolast.invernaderos.features.telemetry.timescaledb.entities.SensorReadingId
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.Instant

@Repository
interface SensorReadingRepository : JpaRepository<SensorReading, SensorReadingId> {

    /**
     * Obtiene el historial de un código específico ordenado por tiempo descendente
     */
    @Query("SELECT sr FROM SensorReading sr WHERE sr.code = :code ORDER BY sr.time DESC")
    fun findByCodeOrderByTimeDesc(@Param("code") code: String): List<SensorReading>

    /**
     * Obtiene el historial de un código en un rango de tiempo
     */
    @Query("SELECT sr FROM SensorReading sr WHERE sr.code = :code AND sr.time BETWEEN :start AND :end ORDER BY sr.time DESC")
    fun findByCodeAndTimeBetween(
        @Param("code") code: String,
        @Param("start") start: Instant,
        @Param("end") end: Instant
    ): List<SensorReading>

    /**
     * Obtiene los últimos N registros ordenados por tiempo descendente
     */
    @Query("SELECT sr FROM SensorReading sr ORDER BY sr.time DESC LIMIT :limit")
    fun findTopNOrderByTimeDesc(@Param("limit") limit: Int): List<SensorReading>

    /**
     * Obtiene el último valor registrado para un código
     */
    @Query(value = """
        SELECT * FROM iot.sensor_readings
        WHERE code = :code
        ORDER BY time DESC
        LIMIT 1
    """, nativeQuery = true)
    fun findLatestByCode(@Param("code") code: String): SensorReading?

    /**
     * Obtiene el último valor registrado para cada código
     */
    @Query(value = """
        SELECT DISTINCT ON (code) *
        FROM iot.sensor_readings
        ORDER BY code, time DESC
    """, nativeQuery = true)
    fun findLatestForAllCodes(): List<SensorReading>

    /**
     * Obtiene lecturas en un rango de tiempo
     */
    @Query("SELECT sr FROM SensorReading sr WHERE sr.time BETWEEN :start AND :end ORDER BY sr.time DESC")
    fun findByTimeBetween(
        @Param("start") start: Instant,
        @Param("end") end: Instant
    ): List<SensorReading>

    /**
     * Obtiene todos los códigos distintos
     */
    @Query("SELECT DISTINCT sr.code FROM SensorReading sr")
    fun findDistinctCodes(): List<String>

    /**
     * Cuenta registros en un rango de tiempo
     */
    @Query("SELECT COUNT(DISTINCT sr.time) FROM SensorReading sr WHERE sr.time BETWEEN :start AND :end")
    fun countByTimeBetween(
        @Param("start") start: Instant,
        @Param("end") end: Instant
    ): Long
}
