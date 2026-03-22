package com.apptolast.invernaderos.features.device.infrastructure.adapter.output

import com.apptolast.invernaderos.features.device.domain.port.output.SectorExistencePort
import com.apptolast.invernaderos.features.sector.SectorRepository
import com.apptolast.invernaderos.features.shared.domain.model.SectorId
import com.apptolast.invernaderos.features.shared.domain.model.TenantId
import org.springframework.stereotype.Component

@Component
class SectorExistenceAdapter(
    private val sectorRepository: SectorRepository
) : SectorExistencePort {

    override fun existsByIdAndTenantId(id: SectorId, tenantId: TenantId): Boolean {
        val sector = sectorRepository.findById(id.value).orElse(null) ?: return false
        return sector.tenantId == tenantId.value
    }
}
