package com.apptolast.invernaderos.features.alert

import io.hypersistence.utils.hibernate.id.Tsid
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

/**
 * JPA entity for the metadata.alert_state_changes audit table.
 * Records every is_resolved transition for an Alert, regardless of source.
 *
 * The [source] column is a plain String (CHECK constraint in SQL) intentionally —
 * we avoid @Enumerated here to keep this JPA entity decoupled from the domain enum.
 * Conversion happens in the mapper layer via AlertSignalSource.name / valueOf.
 */
@Entity
@Table(name = "alert_state_changes", schema = "metadata")
data class AlertStateChange(
    @Id
    @Tsid
    var id: Long? = null,

    @Column(name = "alert_id", nullable = false)
    val alertId: Long,

    @Column(name = "from_resolved", nullable = false)
    val fromResolved: Boolean,

    @Column(name = "to_resolved", nullable = false)
    val toResolved: Boolean,

    @Column(length = 16, nullable = false)
    val source: String,

    @Column(name = "raw_value", length = 64)
    val rawValue: String? = null,

    @Column(nullable = false)
    val at: Instant = Instant.now()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AlertStateChange) return false
        return id != null && id == other.id
    }

    override fun hashCode(): Int = id?.hashCode() ?: 0
}
