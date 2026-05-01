package com.apptolast.invernaderos.features.setting.domain.usecase

import com.apptolast.invernaderos.features.setting.application.usecase.DeleteSettingUseCaseImpl
import com.apptolast.invernaderos.features.setting.domain.error.SettingError
import com.apptolast.invernaderos.features.setting.domain.model.Setting
import com.apptolast.invernaderos.features.setting.domain.port.output.SettingRepositoryPort
import com.apptolast.invernaderos.features.shared.domain.Either
import com.apptolast.invernaderos.features.shared.domain.model.SectorId
import com.apptolast.invernaderos.features.shared.domain.model.SettingId
import com.apptolast.invernaderos.features.shared.domain.model.TenantId
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Instant

class DeleteSettingUseCaseTest {

    private val repository = mockk<SettingRepositoryPort>()
    private val applicationEventPublisher = mockk<org.springframework.context.ApplicationEventPublisher>(relaxed = true)
    private val useCase = DeleteSettingUseCaseImpl(repository, applicationEventPublisher)

    private val existing = Setting(
        id = SettingId(1L),
        code = "SET-00001",
        tenantId = TenantId(10L),
        sectorId = SectorId(20L),
        parameterId = 1,
        parameterName = null,
        actuatorStateId = null,
        actuatorStateName = null,
        dataTypeId = null,
        dataTypeName = null,
        value = null,
        description = null,
        clientName = null,
        isActive = true,
        createdAt = Instant.now(),
        updatedAt = Instant.now()
    )

    @Test
    fun `should delete setting when it exists`() {
        every { repository.findByIdAndTenantId(SettingId(1L), TenantId(10L)) } returns existing
        every { repository.delete(SettingId(1L), TenantId(10L)) } returns true

        val result = useCase.execute(SettingId(1L), TenantId(10L))

        assertThat(result).isInstanceOf(Either.Right::class.java)
        verify(exactly = 1) { repository.delete(SettingId(1L), TenantId(10L)) }
    }

    @Test
    fun `should return NotFound when setting does not exist`() {
        every { repository.findByIdAndTenantId(SettingId(999L), TenantId(10L)) } returns null

        val result = useCase.execute(SettingId(999L), TenantId(10L))

        assertThat(result).isInstanceOf(Either.Left::class.java)
        assertThat((result as Either.Left).value).isInstanceOf(SettingError.NotFound::class.java)
        verify(exactly = 0) { repository.delete(any(), any()) }
    }
}
