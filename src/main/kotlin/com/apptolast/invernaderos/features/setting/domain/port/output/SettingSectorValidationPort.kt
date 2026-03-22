package com.apptolast.invernaderos.features.setting.domain.port.output

import com.apptolast.invernaderos.features.shared.domain.model.SectorId
import com.apptolast.invernaderos.features.shared.domain.model.TenantId

interface SettingSectorValidationPort {
    fun existsByIdAndTenantId(sectorId: SectorId, tenantId: TenantId): Boolean
}
