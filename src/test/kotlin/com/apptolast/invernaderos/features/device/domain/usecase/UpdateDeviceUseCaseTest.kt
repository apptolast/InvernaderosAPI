package com.apptolast.invernaderos.features.device.domain.usecase

import com.apptolast.invernaderos.features.device.application.usecase.UpdateDeviceUseCaseImpl
import com.apptolast.invernaderos.features.device.domain.error.DeviceError
import com.apptolast.invernaderos.features.device.domain.model.Device
import com.apptolast.invernaderos.features.device.domain.port.input.UpdateDeviceCommand
import com.apptolast.invernaderos.features.device.domain.port.output.DeviceRepositoryPort
import com.apptolast.invernaderos.features.device.domain.port.output.SectorExistencePort
import com.apptolast.invernaderos.features.shared.domain.Either
import com.apptolast.invernaderos.features.shared.domain.model.DeviceId
import com.apptolast.invernaderos.features.shared.domain.model.SectorId
import com.apptolast.invernaderos.features.shared.domain.model.TenantId
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Instant

class UpdateDeviceUseCaseTest {

    private val repository = mockk<DeviceRepositoryPort>()
    private val sectorExistence = mockk<SectorExistencePort>()
    private val applicationEventPublisher = mockk<org.springframework.context.ApplicationEventPublisher>(relaxed = true)
    private val useCase = UpdateDeviceUseCaseImpl(repository, sectorExistence, applicationEventPublisher)

    private val existing = Device(
        id = DeviceId(1L), code = "DEV-00001", tenantId = TenantId(10L), sectorId = SectorId(20L),
        sectorCode = "SEC-00001", name = "Sensor Temp", categoryId = 1, categoryName = "SENSOR",
        typeId = 1, typeName = "TEMPERATURE", unitId = 1, unitSymbol = "°C",
        clientName = null,
        isActive = true, createdAt = Instant.parse("2026-01-01T00:00:00Z"), updatedAt = Instant.parse("2026-01-01T00:00:00Z")
    )

    @Test
    fun `should update device name`() {
        val command = UpdateDeviceCommand(id = DeviceId(1L), tenantId = TenantId(10L), name = "Sensor Humedad")
        every { repository.findByIdAndTenantId(DeviceId(1L), TenantId(10L)) } returns existing
        every { repository.save(any()) } answers { firstArg() }

        val result = useCase.execute(command)
        assertThat(result).isInstanceOf(Either.Right::class.java)
        assertThat((result as Either.Right).value.name).isEqualTo("Sensor Humedad")
    }

    @Test
    fun `should return NotFound when device does not exist`() {
        val command = UpdateDeviceCommand(id = DeviceId(999L), tenantId = TenantId(10L))
        every { repository.findByIdAndTenantId(DeviceId(999L), TenantId(10L)) } returns null

        val result = useCase.execute(command)
        assertThat(result).isInstanceOf(Either.Left::class.java)
        assertThat((result as Either.Left).value).isInstanceOf(DeviceError.NotFound::class.java)
    }

    @Test
    fun `should return error when new sector does not belong to tenant`() {
        val command = UpdateDeviceCommand(id = DeviceId(1L), tenantId = TenantId(10L), sectorId = SectorId(99L))
        every { repository.findByIdAndTenantId(DeviceId(1L), TenantId(10L)) } returns existing
        every { sectorExistence.existsByIdAndTenantId(SectorId(99L), TenantId(10L)) } returns false

        val result = useCase.execute(command)
        assertThat(result).isInstanceOf(Either.Left::class.java)
        assertThat((result as Either.Left).value).isInstanceOf(DeviceError.SectorNotOwnedByTenant::class.java)
        verify(exactly = 0) { repository.save(any()) }
    }
}
