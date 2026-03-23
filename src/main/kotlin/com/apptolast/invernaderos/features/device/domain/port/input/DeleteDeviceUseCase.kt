package com.apptolast.invernaderos.features.device.domain.port.input

import com.apptolast.invernaderos.features.device.domain.error.DeviceError
import com.apptolast.invernaderos.features.shared.domain.Either
import com.apptolast.invernaderos.features.shared.domain.model.DeviceId
import com.apptolast.invernaderos.features.shared.domain.model.TenantId

interface DeleteDeviceUseCase {
    fun execute(id: DeviceId, tenantId: TenantId): Either<DeviceError, Unit>
}
