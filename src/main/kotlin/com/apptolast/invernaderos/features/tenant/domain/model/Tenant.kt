package com.apptolast.invernaderos.features.tenant.domain.model

import com.apptolast.invernaderos.features.shared.domain.model.Location
import com.apptolast.invernaderos.features.shared.domain.model.TenantId
import java.time.Instant

data class Tenant(
    val id: TenantId?,
    val code: String,
    val name: String,
    val email: String,
    val phone: String?,
    val province: String?,
    val country: String?,
    val location: Location?,
    val status: TenantStatus,
    val createdAt: Instant,
    val updatedAt: Instant
)
