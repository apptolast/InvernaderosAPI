package com.apptolast.invernaderos.features.device.domain.error

import com.apptolast.invernaderos.features.shared.domain.model.DeviceId
import com.apptolast.invernaderos.features.shared.domain.model.SectorId
import com.apptolast.invernaderos.features.shared.domain.model.TenantId

sealed interface DeviceError {
    val message: String

    data class NotFound(val id: DeviceId, val tenantId: TenantId) : DeviceError {
        override val message: String
            get() = "Device ${id.value} not found for tenant ${tenantId.value}"
    }

    data class SectorNotOwnedByTenant(val sectorId: SectorId, val tenantId: TenantId) : DeviceError {
        override val message: String
            get() = "Sector ${sectorId.value} does not belong to tenant ${tenantId.value}"
    }
}
