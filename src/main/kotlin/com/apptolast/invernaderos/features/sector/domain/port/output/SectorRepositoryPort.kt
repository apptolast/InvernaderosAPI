package com.apptolast.invernaderos.features.sector.domain.port.output

import com.apptolast.invernaderos.features.sector.domain.model.Sector
import com.apptolast.invernaderos.features.shared.domain.model.GreenhouseId
import com.apptolast.invernaderos.features.shared.domain.model.SectorId
import com.apptolast.invernaderos.features.shared.domain.model.TenantId

interface SectorRepositoryPort {
    fun findByIdAndTenantId(id: SectorId, tenantId: TenantId): Sector?
    fun findAllByTenantId(tenantId: TenantId): List<Sector>
    fun findAllByGreenhouseId(greenhouseId: GreenhouseId): List<Sector>
    fun save(sector: Sector): Sector
    fun delete(id: SectorId, tenantId: TenantId): Boolean
}
