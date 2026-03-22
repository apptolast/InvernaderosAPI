package com.apptolast.invernaderos.features.device.domain.port.input

import com.apptolast.invernaderos.features.device.domain.error.DeviceError
import com.apptolast.invernaderos.features.device.domain.model.Device
import com.apptolast.invernaderos.features.shared.domain.Either
import com.apptolast.invernaderos.features.shared.domain.model.SectorId
import com.apptolast.invernaderos.features.shared.domain.model.TenantId

interface CreateDeviceUseCase {
    fun execute(command: CreateDeviceCommand): Either<DeviceError, Device>
}

data class CreateDeviceCommand(
    val tenantId: TenantId,
    val sectorId: SectorId,
    val name: String? = null,
    val categoryId: Short? = null,
    val typeId: Short? = null,
    val unitId: Short? = null,
    val isActive: Boolean = true
)
