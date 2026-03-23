package com.apptolast.invernaderos.features.sector.application.usecase

import com.apptolast.invernaderos.features.sector.domain.error.SectorError
import com.apptolast.invernaderos.features.sector.domain.model.Sector
import com.apptolast.invernaderos.features.sector.domain.port.input.FindSectorUseCase
import com.apptolast.invernaderos.features.sector.domain.port.output.SectorRepositoryPort
import com.apptolast.invernaderos.features.shared.domain.Either
import com.apptolast.invernaderos.features.shared.domain.model.GreenhouseId
import com.apptolast.invernaderos.features.shared.domain.model.SectorId
import com.apptolast.invernaderos.features.shared.domain.model.TenantId

class FindSectorUseCaseImpl(
    private val repository: SectorRepositoryPort
) : FindSectorUseCase {

    override fun findByIdAndTenantId(id: SectorId, tenantId: TenantId): Either<SectorError, Sector> {
        val sector = repository.findByIdAndTenantId(id, tenantId)
            ?: return Either.Left(SectorError.NotFound(id, tenantId))
        return Either.Right(sector)
    }

    override fun findAllByTenantId(tenantId: TenantId): List<Sector> {
        return repository.findAllByTenantId(tenantId)
    }

    override fun findAllByGreenhouseId(greenhouseId: GreenhouseId): List<Sector> {
        return repository.findAllByGreenhouseId(greenhouseId)
    }
}
