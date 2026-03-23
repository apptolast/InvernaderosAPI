package com.apptolast.invernaderos.features.setting.domain.port.input

import com.apptolast.invernaderos.features.setting.domain.error.SettingError
import com.apptolast.invernaderos.features.setting.domain.model.Setting
import com.apptolast.invernaderos.features.shared.domain.Either
import com.apptolast.invernaderos.features.shared.domain.model.SectorId
import com.apptolast.invernaderos.features.shared.domain.model.TenantId

interface CreateSettingUseCase {
    fun execute(command: CreateSettingCommand): Either<SettingError, Setting>
}

data class CreateSettingCommand(
    val tenantId: TenantId,
    val sectorId: SectorId,
    val parameterId: Short,
    val actuatorStateId: Short? = null,
    val dataTypeId: Short? = null,
    val value: String? = null,
    val description: String? = null,
    val clientName: String? = null,
    val isActive: Boolean = true
)
