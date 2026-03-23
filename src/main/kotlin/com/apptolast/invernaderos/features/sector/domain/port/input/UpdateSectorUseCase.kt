package com.apptolast.invernaderos.features.sector.domain.port.input

import com.apptolast.invernaderos.features.sector.domain.error.SectorError
import com.apptolast.invernaderos.features.sector.domain.model.Sector
import com.apptolast.invernaderos.features.shared.domain.Either
import com.apptolast.invernaderos.features.shared.domain.model.GreenhouseId
import com.apptolast.invernaderos.features.shared.domain.model.SectorId
import com.apptolast.invernaderos.features.shared.domain.model.TenantId

interface UpdateSectorUseCase {
    fun execute(command: UpdateSectorCommand): Either<SectorError, Sector>
}

data class UpdateSectorCommand(
    val id: SectorId,
    val tenantId: TenantId,
    val greenhouseId: GreenhouseId? = null,
    val name: String? = null
)
