package com.apptolast.invernaderos.features.device

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.UUID

@Repository
interface CommandHistoryRepository : JpaRepository<CommandHistory, UUID> {

    @EntityGraph(value = "CommandHistory.context")
    fun findByDeviceId(deviceId: UUID): List<CommandHistory>

    @EntityGraph(value = "CommandHistory.context")
    fun findByDeviceIdOrderByCreatedAtDesc(deviceId: UUID): List<CommandHistory>

    fun findByDeviceIdAndCreatedAtBetween(
        deviceId: UUID,
        start: Instant,
        end: Instant
    ): List<CommandHistory>

    fun findByUserId(userId: UUID): List<CommandHistory>

    fun findBySource(source: CommandSource): List<CommandHistory>

    fun findBySuccess(success: Boolean): List<CommandHistory>

    @Query("SELECT c FROM CommandHistory c WHERE c.deviceId = :deviceId ORDER BY c.createdAt DESC")
    fun findLatestByDeviceId(deviceId: UUID, pageable: Pageable): Page<CommandHistory>

    @Query("SELECT c FROM CommandHistory c WHERE c.deviceId = :deviceId AND c.success = true ORDER BY c.createdAt DESC")
    fun findSuccessfulByDeviceId(deviceId: UUID): List<CommandHistory>

    @Query("SELECT c FROM CommandHistory c WHERE c.deviceId = :deviceId AND c.success = false ORDER BY c.createdAt DESC")
    fun findFailedByDeviceId(deviceId: UUID): List<CommandHistory>

    @Query("SELECT COUNT(c) FROM CommandHistory c WHERE c.deviceId = :deviceId")
    fun countByDeviceId(deviceId: UUID): Long

    @Query("SELECT COUNT(c) FROM CommandHistory c WHERE c.deviceId = :deviceId AND c.success = true")
    fun countSuccessfulByDeviceId(deviceId: UUID): Long

    @Query("""
        SELECT c FROM CommandHistory c
        WHERE c.deviceId IN (
            SELECT d.id FROM Device d WHERE d.greenhouseId = :greenhouseId
        )
        ORDER BY c.createdAt DESC
    """)
    fun findByGreenhouseId(greenhouseId: UUID): List<CommandHistory>
}
