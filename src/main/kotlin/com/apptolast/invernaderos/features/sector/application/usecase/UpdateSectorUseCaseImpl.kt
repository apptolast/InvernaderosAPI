package com.apptolast.invernaderos.features.sector.application.usecase

import com.apptolast.invernaderos.features.sector.domain.error.SectorError
import com.apptolast.invernaderos.features.sector.domain.model.Sector
import com.apptolast.invernaderos.features.sector.domain.port.input.UpdateSectorCommand
import com.apptolast.invernaderos.features.sector.domain.port.input.UpdateSectorUseCase
import com.apptolast.invernaderos.features.sector.domain.port.output.GreenhouseExistencePort
import com.apptolast.invernaderos.features.sector.domain.port.output.SectorRepositoryPort
import com.apptolast.invernaderos.features.shared.domain.Either
import com.apptolast.invernaderos.features.websocket.event.TenantStatusChangedEvent
import org.springframework.context.ApplicationEventPublisher

class UpdateSectorUseCaseImpl(
    private val repository: SectorRepositoryPort,
    private val greenhouseExistence: GreenhouseExistencePort,
    private val applicationEventPublisher: ApplicationEventPublisher
) : UpdateSectorUseCase {

    override fun execute(command: UpdateSectorCommand): Either<SectorError, Sector> {
        val existing = repository.findByIdAndTenantId(command.id, command.tenantId)
            ?: return Either.Left(SectorError.NotFound(command.id, command.tenantId))

        // Validate new greenhouse if provided and different
        val newGreenhouseId = command.greenhouseId
        if (newGreenhouseId != null && newGreenhouseId != existing.greenhouseId) {
            if (!greenhouseExistence.existsByIdAndTenantId(newGreenhouseId, command.tenantId)) {
                return Either.Left(SectorError.GreenhouseNotOwnedByTenant(newGreenhouseId, command.tenantId))
            }
        }

        val updated = existing.copy(
            greenhouseId = command.greenhouseId ?: existing.greenhouseId,
            name = command.name ?: existing.name
        )

        val saved = repository.save(updated)
        applicationEventPublisher.publishEvent(
            TenantStatusChangedEvent(command.tenantId.value, TenantStatusChangedEvent.Source.SECTOR_CRUD)
        )
        return Either.Right(saved)
    }
}
