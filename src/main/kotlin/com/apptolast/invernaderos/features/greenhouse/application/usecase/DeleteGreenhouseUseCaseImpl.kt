package com.apptolast.invernaderos.features.greenhouse.application.usecase

import com.apptolast.invernaderos.features.greenhouse.domain.error.GreenhouseError
import com.apptolast.invernaderos.features.shared.domain.model.GreenhouseId
import com.apptolast.invernaderos.features.greenhouse.domain.port.input.DeleteGreenhouseUseCase
import com.apptolast.invernaderos.features.greenhouse.domain.port.output.GreenhouseRepositoryPort
import com.apptolast.invernaderos.features.shared.domain.Either
import com.apptolast.invernaderos.features.shared.domain.model.TenantId
import com.apptolast.invernaderos.features.websocket.event.TenantStatusChangedEvent
import org.springframework.context.ApplicationEventPublisher

class DeleteGreenhouseUseCaseImpl(
    private val repository: GreenhouseRepositoryPort,
    private val applicationEventPublisher: ApplicationEventPublisher
) : DeleteGreenhouseUseCase {

    override fun execute(id: GreenhouseId, tenantId: TenantId): Either<GreenhouseError, Unit> {
        if (repository.findByIdAndTenantId(id, tenantId) == null) {
            return Either.Left(GreenhouseError.NotFound(id, tenantId))
        }
        repository.delete(id, tenantId)
        applicationEventPublisher.publishEvent(
            TenantStatusChangedEvent(tenantId.value, TenantStatusChangedEvent.Source.GREENHOUSE_CRUD)
        )
        return Either.Right(Unit)
    }
}
