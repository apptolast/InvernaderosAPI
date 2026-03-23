package com.apptolast.invernaderos.features.alert.domain.port.output

import com.apptolast.invernaderos.features.shared.domain.model.SectorId
import com.apptolast.invernaderos.features.shared.domain.model.TenantId

interface AlertSectorValidationPort {
    fun existsByIdAndTenantId(sectorId: SectorId, tenantId: TenantId): Boolean
}
