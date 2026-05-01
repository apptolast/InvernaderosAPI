package com.apptolast.invernaderos.features.sector.application.usecase

import com.apptolast.invernaderos.features.sector.domain.error.SectorError
import com.apptolast.invernaderos.features.sector.domain.model.Sector
import com.apptolast.invernaderos.features.sector.domain.port.input.CreateSectorCommand
import com.apptolast.invernaderos.features.sector.domain.port.input.CreateSectorUseCase
import com.apptolast.invernaderos.features.sector.domain.port.output.GreenhouseExistencePort
import com.apptolast.invernaderos.features.sector.domain.port.output.SectorCodeGenerator
import com.apptolast.invernaderos.features.sector.domain.port.output.SectorRepositoryPort
import com.apptolast.invernaderos.features.shared.domain.Either
import com.apptolast.invernaderos.features.websocket.event.TenantStatusChangedEvent
import org.springframework.context.ApplicationEventPublisher

class CreateSectorUseCaseImpl(
    private val repository: SectorRepositoryPort,
    private val codeGenerator: SectorCodeGenerator,
    private val greenhouseExistence: GreenhouseExistencePort,
    private val applicationEventPublisher: ApplicationEventPublisher
) : CreateSectorUseCase {

    override fun execute(command: CreateSectorCommand): Either<SectorError, Sector> {
        if (!greenhouseExistence.existsByIdAndTenantId(command.greenhouseId, command.tenantId)) {
            return Either.Left(SectorError.GreenhouseNotOwnedByTenant(command.greenhouseId, command.tenantId))
        }

        val sector = Sector(
            id = null,
            code = codeGenerator.generate(),
            tenantId = command.tenantId,
            greenhouseId = command.greenhouseId,
            greenhouseCode = null,
            name = command.name
        )

        val saved = repository.save(sector)
        applicationEventPublisher.publishEvent(
            TenantStatusChangedEvent(command.tenantId.value, TenantStatusChangedEvent.Source.SECTOR_CRUD)
        )
        return Either.Right(saved)
    }
}
