package com.apptolast.invernaderos.features.device

import java.time.Instant
import java.util.UUID
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface CommandHistoryRepository : JpaRepository<CommandHistory, UUID> {
    fun findByDeviceId(deviceId: UUID): List<CommandHistory>
    fun findByDeviceIdOrderByCreatedAtDesc(deviceId: UUID): List<CommandHistory>
    fun findBySource(source: String): List<CommandHistory>
    fun findBySuccess(success: Boolean): List<CommandHistory>
    fun findByCreatedAtAfter(createdAt: Instant): List<CommandHistory>
    fun findByDeviceIdAndCreatedAtBetween(deviceId: UUID, start: Instant, end: Instant): List<CommandHistory>

    @Query("""
        SELECT ch FROM CommandHistory ch
        WHERE ch.deviceId = :deviceId
        ORDER BY ch.createdAt DESC
        LIMIT :limit
    """)
    fun findRecentByDevice(@Param("deviceId") deviceId: UUID, @Param("limit") limit: Int): List<CommandHistory>
}
