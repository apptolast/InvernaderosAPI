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
}