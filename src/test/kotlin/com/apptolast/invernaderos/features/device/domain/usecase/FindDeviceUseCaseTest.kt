package com.apptolast.invernaderos.features.device.domain.usecase

import com.apptolast.invernaderos.features.device.application.usecase.FindDeviceUseCaseImpl
import com.apptolast.invernaderos.features.device.domain.error.DeviceError
import com.apptolast.invernaderos.features.device.domain.model.Device
import com.apptolast.invernaderos.features.device.domain.port.output.DeviceRepositoryPort
import com.apptolast.invernaderos.features.shared.domain.Either
import com.apptolast.invernaderos.features.shared.domain.model.DeviceId
import com.apptolast.invernaderos.features.shared.domain.model.SectorId
import com.apptolast.invernaderos.features.shared.domain.model.TenantId
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Instant

class FindDeviceUseCaseTest {

    private val repository = mockk<DeviceRepositoryPort>()
    private val useCase = FindDeviceUseCaseImpl(repository)

    private val sample = Device(
        id = DeviceId(1L), code = "DEV-00001", tenantId = TenantId(10L), sectorId = SectorId(20L),
        sectorCode = "SEC-00001", name = "Sensor Temp", categoryId = 1, categoryName = "SENSOR",
        typeId = 1, typeName = "TEMPERATURE", unitId = 1, unitSymbol = "°C",
        clientName = null,
        isActive = true, createdAt = Instant.now(), updatedAt = Instant.now()
    )

    @Test
    fun `should find device by id and tenant`() {
        every { repository.findByIdAndTenantId(DeviceId(1L), TenantId(10L)) } returns sample
        val result = useCase.findByIdAndTenantId(DeviceId(1L), TenantId(10L))
        assertThat(result).isInstanceOf(Either.Right::class.java)
        assertThat((result as Either.Right).value.name).isEqualTo("Sensor Temp")
    }

    @Test
    fun `should return NotFound when device does not exist`() {
        every { repository.findByIdAndTenantId(DeviceId(999L), TenantId(10L)) } returns null
        val result = useCase.findByIdAndTenantId(DeviceId(999L), TenantId(10L))
        assertThat(result).isInstanceOf(Either.Left::class.java)
        assertThat((result as Either.Left).value).isInstanceOf(DeviceError.NotFound::class.java)
    }

    @Test
    fun `should find all devices by tenant`() {
        every { repository.findAllByTenantId(TenantId(10L)) } returns listOf(sample)
        assertThat(useCase.findAllByTenantId(TenantId(10L))).hasSize(1)
    }

    @Test
    fun `should find all devices by sector`() {
        every { repository.findAllBySectorId(SectorId(20L)) } returns listOf(sample)
        assertThat(useCase.findAllBySectorId(SectorId(20L))).hasSize(1)
    }
}
