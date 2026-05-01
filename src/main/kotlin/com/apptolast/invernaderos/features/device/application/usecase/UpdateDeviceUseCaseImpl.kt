package com.apptolast.invernaderos.features.device.application.usecase

import com.apptolast.invernaderos.features.device.domain.error.DeviceError
import com.apptolast.invernaderos.features.device.domain.model.Device
import com.apptolast.invernaderos.features.device.domain.port.input.UpdateDeviceCommand
import com.apptolast.invernaderos.features.device.domain.port.input.UpdateDeviceUseCase
import com.apptolast.invernaderos.features.device.domain.port.output.DeviceRepositoryPort
import com.apptolast.invernaderos.features.device.domain.port.output.SectorExistencePort
import com.apptolast.invernaderos.features.shared.domain.Either
import com.apptolast.invernaderos.features.websocket.event.TenantStatusChangedEvent
import org.springframework.context.ApplicationEventPublisher
import java.time.Instant

class UpdateDeviceUseCaseImpl(
    private val repository: DeviceRepositoryPort,
    private val sectorExistence: SectorExistencePort,
    private val applicationEventPublisher: ApplicationEventPublisher
) : UpdateDeviceUseCase {

    override fun execute(command: UpdateDeviceCommand): Either<DeviceError, Device> {
        val existing = repository.findByIdAndTenantId(command.id, command.tenantId)
            ?: return Either.Left(DeviceError.NotFound(command.id, command.tenantId))

        // Validate new sector if provided and different
        val newSectorId = command.sectorId
        if (newSectorId != null && newSectorId != existing.sectorId) {
            if (!sectorExistence.existsByIdAndTenantId(newSectorId, command.tenantId)) {
                return Either.Left(DeviceError.SectorNotOwnedByTenant(newSectorId, command.tenantId))
            }
        }

        val updated = existing.copy(
            sectorId = command.sectorId ?: existing.sectorId,
            name = command.name?.trim() ?: existing.name,
            categoryId = command.categoryId ?: existing.categoryId,
            typeId = command.typeId ?: existing.typeId,
            unitId = command.unitId ?: existing.unitId,
            clientName = command.clientName ?: existing.clientName,
            isActive = command.isActive ?: existing.isActive,
            updatedAt = Instant.now()
        )

        val saved = repository.save(updated)
        applicationEventPublisher.publishEvent(
            TenantStatusChangedEvent(command.tenantId.value, TenantStatusChangedEvent.Source.DEVICE_CRUD)
        )
        return Either.Right(saved)
    }
}
