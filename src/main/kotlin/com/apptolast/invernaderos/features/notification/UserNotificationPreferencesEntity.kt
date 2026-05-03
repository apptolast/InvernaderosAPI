package com.apptolast.invernaderos.features.notification

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.time.LocalTime

/**
 * JPA entity for metadata.user_notification_preferences.
 *
 * Primary key is user_id (not a generated TSID) because the row is owned 1:1 by a user.
 * [preferredChannel] is mapped as @Enumerated(STRING) to a VARCHAR(16) column.
 * [quietHoursStart]/[quietHoursEnd] are nullable LocalTime matching the SQL TIME NULL columns.
 * [updatedAt] is set explicitly by the application on every save (no SQL trigger).
 */
@Entity
@Table(name = "user_notification_preferences", schema = "metadata")
class UserNotificationPreferencesEntity(
    @Id
    @Column(name = "user_id")
    val userId: Long,

    @Column(name = "category_alerts", nullable = false)
    var categoryAlerts: Boolean = true,

    @Column(name = "category_devices", nullable = false)
    var categoryDevices: Boolean = true,

    @Column(name = "category_subscription", nullable = false)
    var categorySubscription: Boolean = true,

    @Column(name = "min_alert_severity", nullable = false)
    var minAlertSeverity: Short = 1,

    @Column(name = "quiet_hours_start")
    var quietHoursStart: LocalTime? = null,

    @Column(name = "quiet_hours_end")
    var quietHoursEnd: LocalTime? = null,

    @Column(name = "quiet_hours_timezone", nullable = false, length = 64)
    var quietHoursTimezone: String = "Europe/Madrid",

    @Column(name = "preferred_channel", nullable = false, length = 16)
    @Enumerated(EnumType.STRING)
    var preferredChannel: PreferredChannelJpa = PreferredChannelJpa.PUSH,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is UserNotificationPreferencesEntity) return false
        return userId == other.userId
    }

    override fun hashCode(): Int = userId.hashCode()
}

/**
 * JPA-layer enum for the preferred_channel column.
 * Mirrors [com.apptolast.invernaderos.features.notification.domain.model.PreferredChannel]
 * but kept separate to avoid the domain depending on the JPA layer.
 */
enum class PreferredChannelJpa {
    PUSH,
    EMAIL,
    SMS,
    WHATSAPP
}
