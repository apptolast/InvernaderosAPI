package com.apptolast.invernaderos.features.device.application.usecase

import com.apptolast.invernaderos.features.device.domain.error.DeviceError
import com.apptolast.invernaderos.features.device.domain.model.Device
import com.apptolast.invernaderos.features.device.domain.port.input.FindDeviceUseCase
import com.apptolast.invernaderos.features.device.domain.port.output.DeviceRepositoryPort
import com.apptolast.invernaderos.features.shared.domain.Either
import com.apptolast.invernaderos.features.shared.domain.model.DeviceId
import com.apptolast.invernaderos.features.shared.domain.model.SectorId
import com.apptolast.invernaderos.features.shared.domain.model.TenantId

class FindDeviceUseCaseImpl(
    private val repository: DeviceRepositoryPort
) : FindDeviceUseCase {

    override fun findByIdAndTenantId(id: DeviceId, tenantId: TenantId): Either<DeviceError, Device> {
        val device = repository.findByIdAndTenantId(id, tenantId)
            ?: return Either.Left(DeviceError.NotFound(id, tenantId))
        return Either.Right(device)
    }

    override fun findAllByTenantId(tenantId: TenantId): List<Device> {
        return repository.findAllByTenantId(tenantId)
    }

    override fun findAllBySectorId(sectorId: SectorId): List<Device> {
        return repository.findAllBySectorId(sectorId)
    }
}
