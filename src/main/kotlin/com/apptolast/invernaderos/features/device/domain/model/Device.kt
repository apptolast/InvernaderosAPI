package com.apptolast.invernaderos.features.device.domain.model

import com.apptolast.invernaderos.features.shared.domain.model.DeviceId
import com.apptolast.invernaderos.features.shared.domain.model.SectorId
import com.apptolast.invernaderos.features.shared.domain.model.TenantId
import java.time.Instant

data class Device(
    val id: DeviceId?,
    val code: String,
    val tenantId: TenantId,
    val sectorId: SectorId,
    val sectorCode: String?,
    val name: String?,
    val categoryId: Short?,
    val categoryName: String?,
    val typeId: Short?,
    val typeName: String?,
    val unitId: Short?,
    val unitSymbol: String?,
    val isActive: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant
)
