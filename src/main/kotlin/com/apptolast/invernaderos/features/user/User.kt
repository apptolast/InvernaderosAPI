package com.apptolast.invernaderos.features.user

import com.apptolast.invernaderos.features.tenant.Tenant
import io.hypersistence.utils.hibernate.id.Tsid
import jakarta.persistence.*
import java.time.Instant

@NamedEntityGraph(name = "User.tenant", attributeNodes = [NamedAttributeNode("tenant")])
@Entity
@Table(
    name = "users",
    schema = "metadata",
    indexes = [
        Index(name = "idx_users_code", columnList = "code")
    ]
)
data class User(
        @Id
        @Tsid
        var id: Long? = null,

        /**
         * Codigo unico legible para identificacion externa.
         * Formato: USR-{numero_padded} (ej: USR-00001)
         */
        @Column(nullable = false, length = 50, unique = true)
        var code: String,

        @Column(name = "tenant_id", nullable = false) val tenantId: Long,
        @Column(nullable = false, length = 50) var username: String,
        @Column(nullable = false, length = 255) var email: String,
        @Column(name = "password_hash", nullable = false, length = 255) var passwordHash: String,
        @Column(nullable = false, length = 20) var role: String,
        @Column(name = "is_active", nullable = false) var isActive: Boolean = true,
        @Column(name = "last_login") var lastLogin: Instant? = null,
        @Column(name = "created_at", nullable = false) val createdAt: Instant = Instant.now(),
        @Column(name = "updated_at", nullable = false) var updatedAt: Instant = Instant.now(),
        @Column(name = "reset_password_token", length = 255) var resetPasswordToken: String? = null,
        @Column(name = "reset_password_token_expiry") var resetPasswordTokenExpiry: Instant? = null
) {
        /** Relación ManyToOne con Tenant. Un usuario pertenece a un tenant. */
        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(
                name = "tenant_id",
                referencedColumnName = "id",
                insertable = false,
                updatable = false
        )
        var tenant: Tenant? = null
}
