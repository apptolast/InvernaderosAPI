package com.apptolast.invernaderos.features.setting.domain.port.input

import com.apptolast.invernaderos.features.setting.domain.error.SettingError
import com.apptolast.invernaderos.features.shared.domain.Either
import com.apptolast.invernaderos.features.shared.domain.model.SettingId
import com.apptolast.invernaderos.features.shared.domain.model.TenantId

interface DeleteSettingUseCase {
    fun execute(id: SettingId, tenantId: TenantId): Either<SettingError, Unit>
}
