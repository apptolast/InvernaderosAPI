package com.apptolast.invernaderos.features.device.domain.port.input

import com.apptolast.invernaderos.features.device.domain.error.DeviceError
import com.apptolast.invernaderos.features.device.domain.model.Device
import com.apptolast.invernaderos.features.shared.domain.Either
import com.apptolast.invernaderos.features.shared.domain.model.DeviceId
import com.apptolast.invernaderos.features.shared.domain.model.SectorId
import com.apptolast.invernaderos.features.shared.domain.model.TenantId

interface FindDeviceUseCase {
    fun findByIdAndTenantId(id: DeviceId, tenantId: TenantId): Either<DeviceError, Device>
    fun findAllByTenantId(tenantId: TenantId): List<Device>
    fun findAllBySectorId(sectorId: SectorId): List<Device>
}
