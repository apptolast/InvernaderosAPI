package com.apptolast.invernaderos.features.telemetry.timeseries

import com.apptolast.invernaderos.features.telemetry.timescaledb.entities.Reading
import com.apptolast.invernaderos.features.telemetry.timescaledb.entities.ReadingId
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.UUID

@Repository
interface ReadingRepository : JpaRepository<Reading, ReadingId> {

    fun findByDeviceId(deviceId: UUID): List<Reading>

    fun findByDeviceIdOrderByTimeDesc(deviceId: UUID): List<Reading>

    fun findByDeviceIdAndTimeBetween(
        deviceId: UUID,
        start: Instant,
        end: Instant
    ): List<Reading>

    fun findByDeviceIdAndTimeBetweenOrderByTimeDesc(
        deviceId: UUID,
        start: Instant,
        end: Instant
    ): List<Reading>

    @Query("SELECT r FROM Reading r WHERE r.deviceId = :deviceId ORDER BY r.time DESC LIMIT 1")
    fun findLatestByDeviceId(deviceId: UUID): Reading?

    @Query("SELECT r FROM Reading r WHERE r.deviceId IN :deviceIds AND r.time BETWEEN :start AND :end ORDER BY r.time DESC")
    fun findByDeviceIdsAndTimeBetween(
        deviceIds: List<UUID>,
        start: Instant,
        end: Instant
    ): List<Reading>

    @Query("SELECT AVG(r.value) FROM Reading r WHERE r.deviceId = :deviceId AND r.time BETWEEN :start AND :end")
    fun findAverageByDeviceIdAndTimeBetween(
        deviceId: UUID,
        start: Instant,
        end: Instant
    ): Double?

    @Query("SELECT MIN(r.value) FROM Reading r WHERE r.deviceId = :deviceId AND r.time BETWEEN :start AND :end")
    fun findMinByDeviceIdAndTimeBetween(
        deviceId: UUID,
        start: Instant,
        end: Instant
    ): Double?

    @Query("SELECT MAX(r.value) FROM Reading r WHERE r.deviceId = :deviceId AND r.time BETWEEN :start AND :end")
    fun findMaxByDeviceIdAndTimeBetween(
        deviceId: UUID,
        start: Instant,
        end: Instant
    ): Double?

    @Query("SELECT COUNT(r) FROM Reading r WHERE r.deviceId = :deviceId AND r.time BETWEEN :start AND :end")
    fun countByDeviceIdAndTimeBetween(
        deviceId: UUID,
        start: Instant,
        end: Instant
    ): Long

    @Query("""
        SELECT r FROM Reading r
        WHERE r.deviceId = :deviceId
        AND r.time >= :since
        ORDER BY r.time DESC
    """)
    fun findRecentByDeviceId(deviceId: UUID, since: Instant): List<Reading>
}
