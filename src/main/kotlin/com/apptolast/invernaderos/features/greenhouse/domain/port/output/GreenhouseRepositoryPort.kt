package com.apptolast.invernaderos.features.greenhouse.domain.port.output

import com.apptolast.invernaderos.features.greenhouse.domain.model.Greenhouse
import com.apptolast.invernaderos.features.shared.domain.model.GreenhouseId
import com.apptolast.invernaderos.features.shared.domain.model.TenantId

interface GreenhouseRepositoryPort {
    fun findByIdAndTenantId(id: GreenhouseId, tenantId: TenantId): Greenhouse?
    fun findAllByTenantId(tenantId: TenantId): List<Greenhouse>
    fun save(greenhouse: Greenhouse): Greenhouse
    fun delete(id: GreenhouseId, tenantId: TenantId): Boolean
    fun existsByNameAndTenantId(name: String, tenantId: TenantId): Boolean
}
