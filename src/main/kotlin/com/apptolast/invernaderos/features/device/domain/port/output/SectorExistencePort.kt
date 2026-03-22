package com.apptolast.invernaderos.features.device.domain.port.output

import com.apptolast.invernaderos.features.shared.domain.model.SectorId
import com.apptolast.invernaderos.features.shared.domain.model.TenantId

interface SectorExistencePort {
    fun existsByIdAndTenantId(id: SectorId, tenantId: TenantId): Boolean
}
