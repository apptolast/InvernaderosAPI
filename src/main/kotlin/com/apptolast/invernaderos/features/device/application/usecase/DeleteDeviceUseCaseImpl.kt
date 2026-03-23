package com.apptolast.invernaderos.features.device.application.usecase

import com.apptolast.invernaderos.features.device.domain.error.DeviceError
import com.apptolast.invernaderos.features.device.domain.port.input.DeleteDeviceUseCase
import com.apptolast.invernaderos.features.device.domain.port.output.DeviceRepositoryPort
import com.apptolast.invernaderos.features.shared.domain.Either
import com.apptolast.invernaderos.features.shared.domain.model.DeviceId
import com.apptolast.invernaderos.features.shared.domain.model.TenantId

class DeleteDeviceUseCaseImpl(
    private val repository: DeviceRepositoryPort
) : DeleteDeviceUseCase {

    override fun execute(id: DeviceId, tenantId: TenantId): Either<DeviceError, Unit> {
        if (repository.findByIdAndTenantId(id, tenantId) == null) {
            return Either.Left(DeviceError.NotFound(id, tenantId))
        }
        repository.delete(id, tenantId)
        return Either.Right(Unit)
    }
}
