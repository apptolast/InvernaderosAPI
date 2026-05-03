package com.apptolast.invernaderos.features.notification

import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.Instant

@Repository
interface NotificationLogJpaRepository : JpaRepository<NotificationLogEntity, Long> {

    /**
     * Cursor-based pagination for GET /api/v1/users/me/notifications.
     * Results are ordered by TSID descending (newest first).
     * [cursor] is exclusive: rows with id strictly less than cursor are returned.
     * Pass null to start from the most recent entry.
     */
    @Query("""
        SELECT n FROM NotificationLogEntity n
        WHERE n.userId = :userId
          AND (:cursor IS NULL OR n.id < :cursor)
        ORDER BY n.id DESC
    """)
    fun listForUser(
        @Param("userId") userId: Long,
        @Param("cursor") cursor: Long?,
        pageable: Pageable
    ): List<NotificationLogEntity>

    /**
     * Deduplication check for the AlertAgingDetector: returns true if a notification
     * of [type] referencing [alertId] was already SENT after [since].
     *
     * The LIKE-based JSONB scan is acceptable at MVP volume.
     * MVP: si el volumen lo justifica, indexar alert_id como columna dedicada en notification_log.
     */
    @Query("""
        SELECT COUNT(n) > 0 FROM NotificationLogEntity n
        WHERE n.notificationType = :type
          AND n.payloadJson LIKE CONCAT('%"alertId":"', :alertId, '"%')
          AND n.status = 'SENT'
          AND n.sentAt > :since
    """)
    fun hasRecentSentForAlert(
        @Param("type") type: String,
        @Param("alertId") alertId: Long,
        @Param("since") since: Instant
    ): Boolean
}
