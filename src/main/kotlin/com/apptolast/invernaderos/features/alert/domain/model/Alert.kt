package com.apptolast.invernaderos.features.alert.domain.model

import com.apptolast.invernaderos.features.shared.domain.model.SectorId
import com.apptolast.invernaderos.features.shared.domain.model.TenantId
import java.time.Instant

data class Alert(
    val id: Long?,
    val code: String,
    val tenantId: TenantId,
    val sectorId: SectorId,
    val sectorCode: String?,
    val alertTypeId: Short?,
    val alertTypeName: String?,
    val severityId: Short?,
    val severityName: String?,
    val severityLevel: Short?,
    val message: String?,
    val description: String?,
    val clientName: String?,
    val isResolved: Boolean,
    val resolvedAt: Instant?,
    val resolvedByUserId: Long?,
    val resolvedByUserName: String?,
    val createdAt: Instant,
    val updatedAt: Instant
)
