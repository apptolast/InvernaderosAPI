package com.apptolast.invernaderos.features.sector.domain.port.input

import com.apptolast.invernaderos.features.sector.domain.error.SectorError
import com.apptolast.invernaderos.features.shared.domain.Either
import com.apptolast.invernaderos.features.shared.domain.model.SectorId
import com.apptolast.invernaderos.features.shared.domain.model.TenantId

interface DeleteSectorUseCase {
    fun execute(id: SectorId, tenantId: TenantId): Either<SectorError, Unit>
}
