package com.apptolast.invernaderos.features.device.domain.port.input

import com.apptolast.invernaderos.features.device.domain.error.DeviceError
import com.apptolast.invernaderos.features.device.domain.model.Device
import com.apptolast.invernaderos.features.shared.domain.Either
import com.apptolast.invernaderos.features.shared.domain.model.DeviceId
import com.apptolast.invernaderos.features.shared.domain.model.SectorId
import com.apptolast.invernaderos.features.shared.domain.model.TenantId

interface UpdateDeviceUseCase {
    fun execute(command: UpdateDeviceCommand): Either<DeviceError, Device>
}

data class UpdateDeviceCommand(
    val id: DeviceId,
    val tenantId: TenantId,
    val sectorId: SectorId? = null,
    val name: String? = null,
    val categoryId: Short? = null,
    val typeId: Short? = null,
    val unitId: Short? = null,
    val clientName: String? = null,
    val isActive: Boolean? = null
)
