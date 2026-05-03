package com.apptolast.invernaderos.features.user.domain.model

import com.apptolast.invernaderos.features.shared.domain.model.TenantId
import java.time.Instant

data class User(
    val id: Long?,
    val code: String,
    val tenantId: TenantId,
    val username: String,
    val email: String,
    val role: UserRole,
    val isActive: Boolean,
    val lastLogin: Instant?,
    val createdAt: Instant,
    val updatedAt: Instant,
    val locale: String = "es-ES"
)
