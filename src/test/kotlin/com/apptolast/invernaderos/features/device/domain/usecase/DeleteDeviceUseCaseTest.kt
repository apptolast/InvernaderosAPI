package com.apptolast.invernaderos.features.device.domain.usecase

import com.apptolast.invernaderos.features.device.application.usecase.DeleteDeviceUseCaseImpl
import com.apptolast.invernaderos.features.device.domain.error.DeviceError
import com.apptolast.invernaderos.features.device.domain.model.Device
import com.apptolast.invernaderos.features.device.domain.port.output.DeviceRepositoryPort
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

class DeleteDeviceUseCaseTest {

    private val repository = mockk<DeviceRepositoryPort>()
    private val applicationEventPublisher = mockk<org.springframework.context.ApplicationEventPublisher>(relaxed = true)
    private val useCase = DeleteDeviceUseCaseImpl(repository, applicationEventPublisher)

    private val existing = Device(
        id = DeviceId(1L), code = "DEV-00001", tenantId = TenantId(10L), sectorId = SectorId(20L),
        sectorCode = null, name = "Sensor", categoryId = null, categoryName = null,
        typeId = null, typeName = null, unitId = null, unitSymbol = null,
        clientName = null,
        isActive = true, createdAt = Instant.now(), updatedAt = Instant.now()
    )

    @Test
    fun `should delete device when it exists`() {
        every { repository.findByIdAndTenantId(DeviceId(1L), TenantId(10L)) } returns existing
        every { repository.delete(DeviceId(1L), TenantId(10L)) } returns true

        val result = useCase.execute(DeviceId(1L), TenantId(10L))
        assertThat(result).isInstanceOf(Either.Right::class.java)
        verify(exactly = 1) { repository.delete(DeviceId(1L), TenantId(10L)) }
    }

    @Test
    fun `should return NotFound when device does not exist`() {
        every { repository.findByIdAndTenantId(DeviceId(999L), TenantId(10L)) } returns null

        val result = useCase.execute(DeviceId(999L), TenantId(10L))
        assertThat(result).isInstanceOf(Either.Left::class.java)
        verify(exactly = 0) { repository.delete(any(), any()) }
    }
}
