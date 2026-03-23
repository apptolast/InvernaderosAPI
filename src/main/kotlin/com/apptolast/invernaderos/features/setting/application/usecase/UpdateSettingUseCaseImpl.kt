package com.apptolast.invernaderos.features.setting.application.usecase

import com.apptolast.invernaderos.features.setting.domain.error.SettingError
import com.apptolast.invernaderos.features.setting.domain.model.Setting
import com.apptolast.invernaderos.features.setting.domain.port.input.UpdateSettingCommand
import com.apptolast.invernaderos.features.setting.domain.port.input.UpdateSettingUseCase
import com.apptolast.invernaderos.features.setting.domain.port.output.SettingRepositoryPort
import com.apptolast.invernaderos.features.setting.domain.port.output.SettingSectorValidationPort
import com.apptolast.invernaderos.features.shared.domain.Either
import java.time.Instant

class UpdateSettingUseCaseImpl(
    private val repository: SettingRepositoryPort,
    private val sectorValidation: SettingSectorValidationPort
) : UpdateSettingUseCase {

    override fun execute(command: UpdateSettingCommand): Either<SettingError, Setting> {
        val existing = repository.findByIdAndTenantId(command.id, command.tenantId)
            ?: return Either.Left(SettingError.NotFound(command.id, command.tenantId))

        val newSectorId = command.sectorId
        if (newSectorId != null && newSectorId != existing.sectorId) {
            if (!sectorValidation.existsByIdAndTenantId(newSectorId, command.tenantId)) {
                return Either.Left(SettingError.SectorNotOwnedByTenant(newSectorId, command.tenantId))
            }
        }

        val updated = existing.copy(
            sectorId = command.sectorId ?: existing.sectorId,
            parameterId = command.parameterId ?: existing.parameterId,
            actuatorStateId = command.actuatorStateId ?: existing.actuatorStateId,
            dataTypeId = command.dataTypeId ?: existing.dataTypeId,
            value = command.value ?: existing.value,
            description = command.description ?: existing.description,
            isActive = command.isActive ?: existing.isActive,
            updatedAt = Instant.now()
        )

        return Either.Right(repository.save(updated))
    }
}
