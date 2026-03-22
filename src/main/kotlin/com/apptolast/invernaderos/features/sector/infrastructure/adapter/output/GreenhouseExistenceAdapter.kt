package com.apptolast.invernaderos.features.sector.infrastructure.adapter.output

import com.apptolast.invernaderos.features.greenhouse.GreenhouseRepository
import com.apptolast.invernaderos.features.sector.domain.port.output.GreenhouseExistencePort
import com.apptolast.invernaderos.features.shared.domain.model.GreenhouseId
import com.apptolast.invernaderos.features.shared.domain.model.TenantId
import org.springframework.stereotype.Component

@Component
class GreenhouseExistenceAdapter(
    private val greenhouseRepository: GreenhouseRepository
) : GreenhouseExistencePort {

    override fun existsByIdAndTenantId(id: GreenhouseId, tenantId: TenantId): Boolean {
        return greenhouseRepository.findByIdAndTenantId(id.value, tenantId.value) != null
    }
}
