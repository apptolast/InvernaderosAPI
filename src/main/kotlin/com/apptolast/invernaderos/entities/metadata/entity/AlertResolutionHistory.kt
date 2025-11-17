package com.apptolast.invernaderos.entities.metadata.entity

import jakarta.persistence.*
import org.hibernate.annotations.Immutable
import java.time.Duration
import java.time.Instant
import java.util.UUID

/**
 * Entity para registrar el historial de resoluciones de alertas.
 * READONLY - No modificable después de crear.
 *
 * Mapeo EXACTO de la tabla metadata.alert_resolution_history.
 * Ver DATABASE_SCHEMA_REFERENCE.md para detalles completos.
 */
@Entity
@Table(name = "alert_resolution_history", schema = "metadata")
@Immutable
data class AlertResolutionHistory(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "alert_id", nullable = false)
    val alertId: UUID,

    @Column(name = "previous_status", length = 20)
    val previousStatus: String? = null,

    @Column(name = "previous_severity_id")
    val previousSeverityId: Short? = null,

    @Column(name = "resolved_by", nullable = false)
    val resolvedBy: UUID,

    @Column(name = "resolved_at", nullable = false)
    val resolvedAt: Instant = Instant.now(),

    @Column(name = "resolution_action", length = 50)
    val resolutionAction: String? = null,

    @Column(name = "resolution_notes", columnDefinition = "TEXT")
    val resolutionNotes: String? = null,

    // ⚠️ Campo actions_taken es text[] (PostgreSQL ARRAY)
    // NO lo mapeamos porque causa problemas con Hibernate validation
    // Si lo necesitas, agregar: implementation("io.hypersistence:hypersistence-utils-hibernate-63:3.7.0")
    // y usar @Type(io.hypersistence.utils.hibernate.type.array.ListArrayType::class)

    @Column(name = "time_to_resolution")
    val timeToResolution: Duration? = null,

    @Column(name = "created_at")
    val createdAt: Instant = Instant.now()
)
