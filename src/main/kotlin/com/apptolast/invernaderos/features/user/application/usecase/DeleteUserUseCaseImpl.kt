package com.apptolast.invernaderos.features.user.application.usecase

import com.apptolast.invernaderos.features.shared.domain.Either
import com.apptolast.invernaderos.features.shared.domain.model.TenantId
import com.apptolast.invernaderos.features.user.domain.error.UserError
import com.apptolast.invernaderos.features.user.domain.port.input.DeleteUserUseCase
import com.apptolast.invernaderos.features.user.domain.port.output.UserRepositoryPort
import com.apptolast.invernaderos.features.websocket.event.TenantStatusChangedEvent
import org.springframework.context.ApplicationEventPublisher

class DeleteUserUseCaseImpl(
    private val repository: UserRepositoryPort,
    private val applicationEventPublisher: ApplicationEventPublisher
) : DeleteUserUseCase {

    override fun execute(id: Long, tenantId: TenantId): Either<UserError, Unit> {
        val exists = repository.findByIdAndTenantId(id, tenantId)
            ?: return Either.Left(UserError.NotFound(id, tenantId))

        repository.delete(exists.id!!, tenantId)
        applicationEventPublisher.publishEvent(
            TenantStatusChangedEvent(tenantId.value, TenantStatusChangedEvent.Source.USER_CRUD)
        )
        return Either.Right(Unit)
    }
}
