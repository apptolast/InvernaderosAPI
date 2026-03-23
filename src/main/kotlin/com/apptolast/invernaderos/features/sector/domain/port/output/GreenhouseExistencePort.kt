package com.apptolast.invernaderos.features.sector.domain.port.output

import com.apptolast.invernaderos.features.shared.domain.model.GreenhouseId
import com.apptolast.invernaderos.features.shared.domain.model.TenantId

interface GreenhouseExistencePort {
    fun existsByIdAndTenantId(id: GreenhouseId, tenantId: TenantId): Boolean
}
