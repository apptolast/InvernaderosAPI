package com.apptolast.invernaderos.features.device.application.usecase

import com.apptolast.invernaderos.features.device.domain.error.DeviceError
import com.apptolast.invernaderos.features.device.domain.model.Device
import com.apptolast.invernaderos.features.device.domain.port.input.CreateDeviceCommand
import com.apptolast.invernaderos.features.device.domain.port.input.CreateDeviceUseCase
import com.apptolast.invernaderos.features.device.domain.port.output.DeviceCodeGenerator
import com.apptolast.invernaderos.features.device.domain.port.output.DeviceRepositoryPort
import com.apptolast.invernaderos.features.device.domain.port.output.SectorExistencePort
import com.apptolast.invernaderos.features.shared.domain.Either
import java.time.Instant

class CreateDeviceUseCaseImpl(
    private val repository: DeviceRepositoryPort,
    private val codeGenerator: DeviceCodeGenerator,
    private val sectorExistence: SectorExistencePort
) : CreateDeviceUseCase {

    override fun execute(command: CreateDeviceCommand): Either<DeviceError, Device> {
        if (!sectorExistence.existsByIdAndTenantId(command.sectorId, command.tenantId)) {
            return Either.Left(DeviceError.SectorNotOwnedByTenant(command.sectorId, command.tenantId))
        }

        val now = Instant.now()
        val device = Device(
            id = null,
            code = codeGenerator.generate(),
            tenantId = command.tenantId,
            sectorId = command.sectorId,
            sectorCode = null,
            name = command.name?.trim(),
            categoryId = command.categoryId,
            categoryName = null,
            typeId = command.typeId,
            typeName = null,
            unitId = command.unitId,
            unitSymbol = null,
            clientName = command.clientName,
            isActive = command.isActive,
            createdAt = now,
            updatedAt = now
        )

        return Either.Right(repository.save(device))
    }
}
