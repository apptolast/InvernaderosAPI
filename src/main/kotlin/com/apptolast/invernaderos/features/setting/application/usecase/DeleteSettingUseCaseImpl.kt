package com.apptolast.invernaderos.features.setting.application.usecase

import com.apptolast.invernaderos.features.setting.domain.error.SettingError
import com.apptolast.invernaderos.features.setting.domain.port.input.DeleteSettingUseCase
import com.apptolast.invernaderos.features.setting.domain.port.output.SettingRepositoryPort
import com.apptolast.invernaderos.features.shared.domain.Either
import com.apptolast.invernaderos.features.shared.domain.model.SettingId
import com.apptolast.invernaderos.features.shared.domain.model.TenantId
import com.apptolast.invernaderos.features.websocket.event.TenantStatusChangedEvent
import org.springframework.context.ApplicationEventPublisher

class DeleteSettingUseCaseImpl(
    private val repository: SettingRepositoryPort,
    private val applicationEventPublisher: ApplicationEventPublisher
) : DeleteSettingUseCase {

    override fun execute(id: SettingId, tenantId: TenantId): Either<SettingError, Unit> {
        if (repository.findByIdAndTenantId(id, tenantId) == null) {
            return Either.Left(SettingError.NotFound(id, tenantId))
        }
        repository.delete(id, tenantId)
        applicationEventPublisher.publishEvent(
            TenantStatusChangedEvent(tenantId.value, TenantStatusChangedEvent.Source.SETTING_CRUD)
        )
        return Either.Right(Unit)
    }
}
