package com.apptolast.invernaderos.features.auth.refresh.infrastructure.persistence.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "refresh_tokens", schema = "metadata")
class RefreshTokenEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(name = "user_id", nullable = false)
    var userId: Long,

    @Column(name = "token_hash", nullable = false, length = 64, unique = true)
    var tokenHash: String,

    @Column(name = "family_id", nullable = false, columnDefinition = "uuid")
    var familyId: UUID,

    @Column(name = "rotated_from_id")
    var rotatedFromId: Long? = null,

    @Column(name = "expires_at", nullable = false)
    var expiresAt: Instant,

    @Column(name = "revoked_at")
    var revokedAt: Instant? = null,

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant
)
