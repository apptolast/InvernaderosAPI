package com.apptolast.invernaderos.features.device.domain.usecase

import com.apptolast.invernaderos.features.device.application.usecase.CreateDeviceUseCaseImpl
import com.apptolast.invernaderos.features.device.domain.error.DeviceError
import com.apptolast.invernaderos.features.device.domain.model.Device
import com.apptolast.invernaderos.features.device.domain.port.input.CreateDeviceCommand
import com.apptolast.invernaderos.features.device.domain.port.output.DeviceCodeGenerator
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

class CreateDeviceUseCaseTest {

    private val repository = mockk<DeviceRepositoryPort>()
    private val codeGenerator = mockk<DeviceCodeGenerator>()
    private val sectorExistence = mockk<SectorExistencePort>()
    private val applicationEventPublisher = mockk<org.springframework.context.ApplicationEventPublisher>(relaxed = true)
    private val useCase = CreateDeviceUseCaseImpl(repository, codeGenerator, sectorExistence, applicationEventPublisher)

    @Test
    fun `should create device when sector belongs to tenant`() {
        val command = CreateDeviceCommand(
            tenantId = TenantId(1L),
            sectorId = SectorId(10L),
            name = "Sensor Temperatura",
            categoryId = 1,
            typeId = 1,
            unitId = 1,
            isActive = true
        )

        every { sectorExistence.existsByIdAndTenantId(SectorId(10L), TenantId(1L)) } returns true
        every { codeGenerator.generate() } returns "DEV-00001"
        every { repository.save(any()) } answers {
            val d = firstArg<Device>()
            d.copy(id = DeviceId(100L), sectorCode = "SEC-00001", categoryName = "SENSOR", typeName = "TEMPERATURE", unitSymbol = "°C")
        }

        val result = useCase.execute(command)

        assertThat(result).isInstanceOf(Either.Right::class.java)
        val device = (result as Either.Right).value
        assertThat(device.id).isEqualTo(DeviceId(100L))
        assertThat(device.code).isEqualTo("DEV-00001")
        assertThat(device.name).isEqualTo("Sensor Temperatura")
        verify(exactly = 1) { repository.save(any()) }
    }

    @Test
    fun `should return error when sector does not belong to tenant`() {
        val command = CreateDeviceCommand(tenantId = TenantId(1L), sectorId = SectorId(99L))

        every { sectorExistence.existsByIdAndTenantId(SectorId(99L), TenantId(1L)) } returns false

        val result = useCase.execute(command)

        assertThat(result).isInstanceOf(Either.Left::class.java)
        assertThat((result as Either.Left).value).isInstanceOf(DeviceError.SectorNotOwnedByTenant::class.java)
        verify(exactly = 0) { repository.save(any()) }
    }
}
