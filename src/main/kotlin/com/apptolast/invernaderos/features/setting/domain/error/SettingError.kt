package com.apptolast.invernaderos.features.setting.domain.error

import com.apptolast.invernaderos.features.shared.domain.model.SectorId
import com.apptolast.invernaderos.features.shared.domain.model.SettingId
import com.apptolast.invernaderos.features.shared.domain.model.TenantId

sealed interface SettingError {
    val message: String

    data class NotFound(val id: SettingId, val tenantId: TenantId) : SettingError {
        override val message: String
            get() = "Setting ${id.value} not found for tenant ${tenantId.value}"
    }

    data class SectorNotOwnedByTenant(val sectorId: SectorId, val tenantId: TenantId) : SettingError {
        override val message: String
            get() = "Sector ${sectorId.value} does not belong to tenant ${tenantId.value}"
    }
}
