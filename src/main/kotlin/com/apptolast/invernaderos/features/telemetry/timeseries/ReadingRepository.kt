package com.apptolast.invernaderos.features.telemetry.timeseries

import com.apptolast.invernaderos.features.telemetry.timescaledb.entities.Reading
import com.apptolast.invernaderos.features.telemetry.timescaledb.entities.ReadingId
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.Instant

@Repository
interface ReadingRepository : JpaRepository<Reading, ReadingId> {

    fun findByDeviceId(deviceId: Long): List<Reading>

    fun findByDeviceIdOrderByTimeDesc(deviceId: Long): List<Reading>

    fun findByDeviceIdAndTimeBetween(
        deviceId: Long,
        start: Instant,
        end: Instant
    ): List<Reading>

    fun findByDeviceIdAndTimeBetweenOrderByTimeDesc(
        deviceId: Long,
        start: Instant,
        end: Instant
    ): List<Reading>

    @Query("SELECT r FROM Reading r WHERE r.deviceId = :deviceId ORDER BY r.time DESC LIMIT 1")
    fun findLatestByDeviceId(deviceId: Long): Reading?

    @Query("SELECT r FROM Reading r WHERE r.deviceId IN :deviceIds AND r.time BETWEEN :start AND :end ORDER BY r.time DESC")
    fun findByDeviceIdsAndTimeBetween(
        deviceIds: List<Long>,
        start: Instant,
        end: Instant
    ): List<Reading>

    @Query("SELECT AVG(r.value) FROM Reading r WHERE r.deviceId = :deviceId AND r.time BETWEEN :start AND :end")
    fun findAverageByDeviceIdAndTimeBetween(
        deviceId: Long,
        start: Instant,
        end: Instant
    ): Double?

    @Query("SELECT MIN(r.value) FROM Reading r WHERE r.deviceId = :deviceId AND r.time BETWEEN :start AND :end")
    fun findMinByDeviceIdAndTimeBetween(
        deviceId: Long,
        start: Instant,
        end: Instant
    ): Double?

    @Query("SELECT MAX(r.value) FROM Reading r WHERE r.deviceId = :deviceId AND r.time BETWEEN :start AND :end")
    fun findMaxByDeviceIdAndTimeBetween(
        deviceId: Long,
        start: Instant,
        end: Instant
    ): Double?

    @Query("SELECT COUNT(r) FROM Reading r WHERE r.deviceId = :deviceId AND r.time BETWEEN :start AND :end")
    fun countByDeviceIdAndTimeBetween(
        deviceId: Long,
        start: Instant,
        end: Instant
    ): Long

    @Query("""
        SELECT r FROM Reading r
        WHERE r.deviceId = :deviceId
        AND r.time >= :since
        ORDER BY r.time DESC
    """)
    fun findRecentByDeviceId(deviceId: Long, since: Instant): List<Reading>
}
