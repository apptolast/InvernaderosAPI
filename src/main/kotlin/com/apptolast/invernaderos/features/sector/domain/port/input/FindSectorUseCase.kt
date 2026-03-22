package com.apptolast.invernaderos.features.sector.domain.port.input

import com.apptolast.invernaderos.features.sector.domain.error.SectorError
import com.apptolast.invernaderos.features.sector.domain.model.Sector
import com.apptolast.invernaderos.features.shared.domain.Either
import com.apptolast.invernaderos.features.shared.domain.model.GreenhouseId
import com.apptolast.invernaderos.features.shared.domain.model.SectorId
import com.apptolast.invernaderos.features.shared.domain.model.TenantId

interface FindSectorUseCase {
    fun findByIdAndTenantId(id: SectorId, tenantId: TenantId): Either<SectorError, Sector>
    fun findAllByTenantId(tenantId: TenantId): List<Sector>
    fun findAllByGreenhouseId(greenhouseId: GreenhouseId): List<Sector>
}
