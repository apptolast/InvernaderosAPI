package com.apptolast.invernaderos.features.sector.domain.port.input

import com.apptolast.invernaderos.features.sector.domain.error.SectorError
import com.apptolast.invernaderos.features.sector.domain.model.Sector
import com.apptolast.invernaderos.features.shared.domain.Either
import com.apptolast.invernaderos.features.shared.domain.model.GreenhouseId
import com.apptolast.invernaderos.features.shared.domain.model.TenantId

interface CreateSectorUseCase {
    fun execute(command: CreateSectorCommand): Either<SectorError, Sector>
}

data class CreateSectorCommand(
    val tenantId: TenantId,
    val greenhouseId: GreenhouseId,
    val name: String? = null
)
