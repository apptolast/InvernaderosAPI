package com.apptolast.invernaderos.features.device.domain.port.output

import com.apptolast.invernaderos.features.device.domain.model.Device
import com.apptolast.invernaderos.features.shared.domain.model.DeviceId
import com.apptolast.invernaderos.features.shared.domain.model.SectorId
import com.apptolast.invernaderos.features.shared.domain.model.TenantId

interface DeviceRepositoryPort {
    fun findByIdAndTenantId(id: DeviceId, tenantId: TenantId): Device?
    fun findAllByTenantId(tenantId: TenantId): List<Device>
    fun findAllBySectorId(sectorId: SectorId): List<Device>
    fun save(device: Device): Device
    fun delete(id: DeviceId, tenantId: TenantId): Boolean
}
