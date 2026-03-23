package com.apptolast.invernaderos.features.greenhouse.domain.model

import com.apptolast.invernaderos.features.shared.domain.model.GreenhouseId
import com.apptolast.invernaderos.features.shared.domain.model.Location
import com.apptolast.invernaderos.features.shared.domain.model.TenantId
import java.math.BigDecimal
import java.time.Instant

data class Greenhouse(
    val id: GreenhouseId?,
    val code: String,
    val tenantId: TenantId,
    val name: String,
    val location: Location?,
    val areaM2: BigDecimal?,
    val timezone: String?,
    val isActive: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant
)
