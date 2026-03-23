package com.apptolast.invernaderos.features.sector.application.usecase

import com.apptolast.invernaderos.features.sector.domain.error.SectorError
import com.apptolast.invernaderos.features.sector.domain.port.input.DeleteSectorUseCase
import com.apptolast.invernaderos.features.sector.domain.port.output.SectorRepositoryPort
import com.apptolast.invernaderos.features.shared.domain.Either
import com.apptolast.invernaderos.features.shared.domain.model.SectorId
import com.apptolast.invernaderos.features.shared.domain.model.TenantId

class DeleteSectorUseCaseImpl(
    private val repository: SectorRepositoryPort
) : DeleteSectorUseCase {

    override fun execute(id: SectorId, tenantId: TenantId): Either<SectorError, Unit> {
        if (repository.findByIdAndTenantId(id, tenantId) == null) {
            return Either.Left(SectorError.NotFound(id, tenantId))
        }
        repository.delete(id, tenantId)
        return Either.Right(Unit)
    }
}
