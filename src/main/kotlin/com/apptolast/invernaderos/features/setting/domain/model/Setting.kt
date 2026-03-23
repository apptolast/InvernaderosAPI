package com.apptolast.invernaderos.features.setting.domain.model

import com.apptolast.invernaderos.features.shared.domain.model.SectorId
import com.apptolast.invernaderos.features.shared.domain.model.SettingId
import com.apptolast.invernaderos.features.shared.domain.model.TenantId
import java.time.Instant

data class Setting(
    val id: SettingId?,
    val code: String,
    val sectorId: SectorId,
    val tenantId: TenantId,
    val parameterId: Short,
    val parameterName: String?,
    val actuatorStateId: Short?,
    val actuatorStateName: String?,
    val dataTypeId: Short?,
    val dataTypeName: String?,
    val value: String?,
    val description: String?,
    val isActive: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant
)
