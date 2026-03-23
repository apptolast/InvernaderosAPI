package com.apptolast.invernaderos.features.setting.domain.port.input

import com.apptolast.invernaderos.features.setting.domain.error.SettingError
import com.apptolast.invernaderos.features.setting.domain.model.Setting
import com.apptolast.invernaderos.features.shared.domain.Either
import com.apptolast.invernaderos.features.shared.domain.model.SectorId
import com.apptolast.invernaderos.features.shared.domain.model.SettingId
import com.apptolast.invernaderos.features.shared.domain.model.TenantId

interface UpdateSettingUseCase {
    fun execute(command: UpdateSettingCommand): Either<SettingError, Setting>
}

data class UpdateSettingCommand(
    val id: SettingId,
    val tenantId: TenantId,
    val sectorId: SectorId? = null,
    val parameterId: Short? = null,
    val actuatorStateId: Short? = null,
    val dataTypeId: Short? = null,
    val value: String? = null,
    val description: String? = null,
    val isActive: Boolean? = null
)
