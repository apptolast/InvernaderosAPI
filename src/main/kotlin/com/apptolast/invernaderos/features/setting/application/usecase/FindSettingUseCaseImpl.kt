package com.apptolast.invernaderos.features.setting.application.usecase

import com.apptolast.invernaderos.features.setting.domain.error.SettingError
import com.apptolast.invernaderos.features.setting.domain.model.Setting
import com.apptolast.invernaderos.features.setting.domain.port.input.FindSettingUseCase
import com.apptolast.invernaderos.features.setting.domain.port.output.SettingRepositoryPort
import com.apptolast.invernaderos.features.shared.domain.Either
import com.apptolast.invernaderos.features.shared.domain.model.SectorId
import com.apptolast.invernaderos.features.shared.domain.model.SettingId
import com.apptolast.invernaderos.features.shared.domain.model.TenantId

class FindSettingUseCaseImpl(
    private val repository: SettingRepositoryPort
) : FindSettingUseCase {

    override fun findByIdAndTenantId(id: SettingId, tenantId: TenantId): Either<SettingError, Setting> {
        val setting = repository.findByIdAndTenantId(id, tenantId)
            ?: return Either.Left(SettingError.NotFound(id, tenantId))
        return Either.Right(setting)
    }

    override fun findAllByTenantId(tenantId: TenantId): List<Setting> {
        return repository.findAllByTenantId(tenantId)
    }

    override fun findAllBySectorId(sectorId: SectorId): List<Setting> {
        return repository.findAllBySectorId(sectorId)
    }

    override fun findActiveBySectorId(sectorId: SectorId): List<Setting> {
        return repository.findActiveBySectorId(sectorId)
    }

    override fun findBySectorIdAndParameterId(sectorId: SectorId, parameterId: Short): List<Setting> {
        return repository.findBySectorIdAndParameterId(sectorId, parameterId)
    }

    override fun findBySectorIdAndActuatorStateId(sectorId: SectorId, actuatorStateId: Short): List<Setting> {
        return repository.findBySectorIdAndActuatorStateId(sectorId, actuatorStateId)
    }

    override fun findBySectorParameterAndActuatorState(
        sectorId: SectorId,
        parameterId: Short,
        actuatorStateId: Short
    ): Either<SettingError, Setting> {
        val setting = repository.findBySectorParameterAndActuatorState(sectorId, parameterId, actuatorStateId)
            ?: return Either.Left(SettingError.NotFound(SettingId(0L), TenantId(0L)))
        return Either.Right(setting)
    }
}
