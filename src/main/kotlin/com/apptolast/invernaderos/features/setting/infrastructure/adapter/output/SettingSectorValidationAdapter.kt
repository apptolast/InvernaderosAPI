package com.apptolast.invernaderos.features.setting.infrastructure.adapter.output

import com.apptolast.invernaderos.features.sector.SectorRepository
import com.apptolast.invernaderos.features.setting.domain.port.output.SettingSectorValidationPort
import com.apptolast.invernaderos.features.shared.domain.model.SectorId
import com.apptolast.invernaderos.features.shared.domain.model.TenantId
import org.springframework.stereotype.Component

@Component
class SettingSectorValidationAdapter(
    private val sectorRepository: SectorRepository
) : SettingSectorValidationPort {

    override fun existsByIdAndTenantId(sectorId: SectorId, tenantId: TenantId): Boolean {
        val sector = sectorRepository.findById(sectorId.value).orElse(null) ?: return false
        return sector.tenantId == tenantId.value
    }
}
