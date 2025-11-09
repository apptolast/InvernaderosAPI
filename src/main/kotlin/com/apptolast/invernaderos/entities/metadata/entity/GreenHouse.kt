package com.apptolast.invernaderos.entities.metadata.entity

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "greenhouses", schema = "public")
data class Greenhouse(
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    val id: UUID? = null,

    @Column(name = "tenant_id", nullable = false)
    val tenantId: UUID,

    @Column(nullable = false, length = 100)
    val name: String,

    @Column(columnDefinition = "jsonb")
    val location: String? = null,

    @Column(name = "area_m2")
    val areaM2: Double? = null,

    @Column(name = "crop_type", length = 50)
    val cropType: String? = null,

    @Column(length = 50)
    val timezone: String? = null,

    @Column(name = "is_active", nullable = false)
    val isActive: Boolean = true,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    val updatedAt: Instant = Instant.now()
)