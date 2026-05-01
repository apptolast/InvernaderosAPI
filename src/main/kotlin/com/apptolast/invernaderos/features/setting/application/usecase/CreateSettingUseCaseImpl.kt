package com.apptolast.invernaderos.features.setting.application.usecase

import com.apptolast.invernaderos.features.setting.domain.error.SettingError
import com.apptolast.invernaderos.features.setting.domain.model.Setting
import com.apptolast.invernaderos.features.setting.domain.port.input.CreateSettingCommand
import com.apptolast.invernaderos.features.setting.domain.port.input.CreateSettingUseCase
import com.apptolast.invernaderos.features.setting.domain.port.output.SettingCodeGenerator
import com.apptolast.invernaderos.features.setting.domain.port.output.SettingRepositoryPort
import com.apptolast.invernaderos.features.setting.domain.port.output.SettingSectorValidationPort
import com.apptolast.invernaderos.features.shared.domain.Either
import com.apptolast.invernaderos.features.websocket.event.TenantStatusChangedEvent
import org.springframework.context.ApplicationEventPublisher
import java.time.Instant

class CreateSettingUseCaseImpl(
    private val repository: SettingRepositoryPort,
    private val codeGenerator: SettingCodeGenerator,
    private val sectorValidation: SettingSectorValidationPort,
    private val applicationEventPublisher: ApplicationEventPublisher
) : CreateSettingUseCase {

    override fun execute(command: CreateSettingCommand): Either<SettingError, Setting> {
        if (!sectorValidation.existsByIdAndTenantId(command.sectorId, command.tenantId)) {
            return Either.Left(SettingError.SectorNotOwnedByTenant(command.sectorId, command.tenantId))
        }

        val now = Instant.now()
        val setting = Setting(
            id = null,
            code = codeGenerator.generate(),
            tenantId = command.tenantId,
            sectorId = command.sectorId,
            parameterId = command.parameterId,
            parameterName = null,
            actuatorStateId = command.actuatorStateId,
            actuatorStateName = null,
            dataTypeId = command.dataTypeId,
            dataTypeName = null,
            value = command.value,
            description = command.description,
            clientName = command.clientName,
            isActive = command.isActive,
            createdAt = now,
            updatedAt = now
        )

        val saved = repository.save(setting)
        applicationEventPublisher.publishEvent(
            TenantStatusChangedEvent(command.tenantId.value, TenantStatusChangedEvent.Source.SETTING_CRUD)
        )
        return Either.Right(saved)
    }
}
