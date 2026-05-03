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
     * Native query because payload_json is JSONB and Hibernate's HQL `LIKE` rejects
     * JSONB columns ("Operand of 'like' is not a string"). The JSONB ->> operator
     * extracts a top-level field as text, which is index-friendly and unambiguous.
     * If notification_log volume grows, index alert_id as a dedicated column.
     */
    @Query(
        value = """
            SELECT EXISTS (
                SELECT 1 FROM metadata.notification_log
                WHERE notification_type = :type
                  AND (payload_json->>'alertId') = CAST(:alertId AS text)
                  AND status = 'SENT'
                  AND sent_at > :since
            )
        """,
        nativeQuery = true
    )
    fun hasRecentSentForAlert(
        @Param("type") type: String,
        @Param("alertId") alertId: Long,
        @Param("since") since: Instant
    ): Boolean
}
