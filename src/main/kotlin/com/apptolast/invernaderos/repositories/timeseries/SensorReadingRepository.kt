package com.apptolast.invernaderos.repositories.timeseries

import com.apptolast.invernaderos.entities.timescaledb.entities.SensorReading
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.Instant

@Repository
interface SensorReadingRepository : JpaRepository<SensorReading, Instant> {

    @Query("SELECT sr FROM SensorReading sr WHERE sr.sensorId = :sensorId AND sr.time BETWEEN :start AND :end ORDER BY sr.time DESC")
    fun findBySensorIdAndTimeBetween(
        @Param("sensorId") sensorId: String,
        @Param("start") start: Instant,
        @Param("end") end: Instant
    ): List<SensorReading>

    @Query("SELECT sr FROM SensorReading sr WHERE sr.greenhouseId = :greenhouseId AND sr.time >= :since ORDER BY sr.time DESC")
    fun findByGreenhouseIdSince(
        @Param("greenhouseId") greenhouseId: String,
        @Param("since") since: Instant
    ): List<SensorReading>

    /**
     * Obtiene los últimos registros ordenados por tiempo descendente
     */
    @Query("SELECT sr FROM SensorReading sr ORDER BY sr.time DESC LIMIT :limit")
    fun findTopNOrderByTimeDesc(@Param("limit") limit: Int): List<SensorReading>

    /**
     * Obtiene el último registro por sensor
     */
    @Query("SELECT sr FROM SensorReading sr ORDER BY sr.time DESC LIMIT 10")
    fun findTopByOrderByTimeDesc(): List<SensorReading>

    /**
     * Obtiene lecturas en un rango de tiempo
     */
    @Query("SELECT sr FROM SensorReading sr WHERE sr.time BETWEEN :start AND :end ORDER BY sr.time DESC")
    fun findByTimeBetween(
        @Param("start") start: Instant,
        @Param("end") end: Instant
    ): List<SensorReading>

    /**
     * Obtiene estadísticas de un sensor en un rango de tiempo
     * Retorna: [sensorId, min, max, avg, count, lastValue, lastTimestamp]
     */
    @Query("""
        SELECT
            sr.sensorId,
            MIN(sr.value),
            MAX(sr.value),
            AVG(sr.value),
            COUNT(sr),
            (SELECT sr2.value FROM SensorReading sr2 WHERE sr2.sensorId = :sensorId ORDER BY sr2.time DESC LIMIT 1),
            (SELECT sr2.time FROM SensorReading sr2 WHERE sr2.sensorId = :sensorId ORDER BY sr2.time DESC LIMIT 1)
        FROM SensorReading sr
        WHERE sr.sensorId = :sensorId
        AND sr.time BETWEEN :startTime AND :endTime
        GROUP BY sr.sensorId
    """)
    fun findStatsBySensorIdAndTimeRange(
        @Param("sensorId") sensorId: String,
        @Param("startTime") startTime: Instant,
        @Param("endTime") endTime: Instant
    ): List<Array<Any>>

    /**
     * Obtiene todos los IDs de sensores distintos
     */
    @Query("SELECT DISTINCT sr.sensorId FROM SensorReading sr")
    fun findDistinctSensorIds(): List<String>

    /**
     * Cuenta mensajes en un rango de tiempo
     */
    @Query("SELECT COUNT(DISTINCT sr.time) FROM SensorReading sr WHERE sr.time BETWEEN :start AND :end")
    fun countByTimeBetween(
        @Param("start") start: Instant,
        @Param("end") end: Instant
    ): Long
}