package com.apptolast.invernaderos.features.setting.domain.port.input

import com.apptolast.invernaderos.features.setting.domain.error.SettingError
import com.apptolast.invernaderos.features.setting.domain.model.Setting
import com.apptolast.invernaderos.features.shared.domain.Either
import com.apptolast.invernaderos.features.shared.domain.model.SectorId
import com.apptolast.invernaderos.features.shared.domain.model.SettingId
import com.apptolast.invernaderos.features.shared.domain.model.TenantId

interface FindSettingUseCase {
    fun findByIdAndTenantId(id: SettingId, tenantId: TenantId): Either<SettingError, Setting>
    fun findAllByTenantId(tenantId: TenantId): List<Setting>
    fun findAllBySectorId(sectorId: SectorId): List<Setting>
    fun findActiveBySectorId(sectorId: SectorId): List<Setting>
    fun findBySectorIdAndParameterId(sectorId: SectorId, parameterId: Short): List<Setting>
    fun findBySectorIdAndActuatorStateId(sectorId: SectorId, actuatorStateId: Short): List<Setting>
    fun findBySectorParameterAndActuatorState(sectorId: SectorId, parameterId: Short, actuatorStateId: Short): Either<SettingError, Setting>
}
