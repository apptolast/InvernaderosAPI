package com.apptolast.invernaderos.features.alert.domain.error

import com.apptolast.invernaderos.features.shared.domain.model.SectorId
import com.apptolast.invernaderos.features.shared.domain.model.TenantId

sealed interface AlertError {
    val message: String

    data class NotFound(val id: Long, val tenantId: TenantId) : AlertError {
        override val message: String
            get() = "Alert $id not found for tenant ${tenantId.value}"
    }

    data class SectorNotOwnedByTenant(val sectorId: SectorId, val tenantId: TenantId) : AlertError {
        override val message: String
            get() = "Sector ${sectorId.value} does not belong to tenant ${tenantId.value}"
    }

    data class AlreadyResolved(val id: Long) : AlertError {
        override val message: String
            get() = "Alert $id is already resolved"
    }
}
