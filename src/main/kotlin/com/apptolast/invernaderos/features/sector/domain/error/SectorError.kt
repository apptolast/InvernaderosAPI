package com.apptolast.invernaderos.features.sector.domain.error

import com.apptolast.invernaderos.features.shared.domain.model.GreenhouseId
import com.apptolast.invernaderos.features.shared.domain.model.SectorId
import com.apptolast.invernaderos.features.shared.domain.model.TenantId

sealed interface SectorError {
    val message: String

    data class NotFound(val id: SectorId, val tenantId: TenantId) : SectorError {
        override val message: String
            get() = "Sector ${id.value} not found for tenant ${tenantId.value}"
    }

    data class GreenhouseNotFound(val greenhouseId: GreenhouseId) : SectorError {
        override val message: String
            get() = "Greenhouse ${greenhouseId.value} not found"
    }

    data class GreenhouseNotOwnedByTenant(val greenhouseId: GreenhouseId, val tenantId: TenantId) : SectorError {
        override val message: String
            get() = "Greenhouse ${greenhouseId.value} does not belong to tenant ${tenantId.value}"
    }
}
