package com.apptolast.invernaderos.entities.metadata.entity

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "tenants", schema = "public")
data class Tenant(
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    val id: UUID? = null,

    @Column(nullable = false, length = 100)
    val name: String,

    @Column(nullable = false, length = 255)
    val email: String,

    @Column(name = "is_active", nullable = false)
    val isActive: Boolean = true,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    val updatedAt: Instant = Instant.now()
)