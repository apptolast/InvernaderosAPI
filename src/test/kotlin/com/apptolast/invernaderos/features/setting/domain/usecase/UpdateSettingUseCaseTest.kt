package com.apptolast.invernaderos.features.setting.domain.usecase

import com.apptolast.invernaderos.features.setting.application.usecase.UpdateSettingUseCaseImpl
import com.apptolast.invernaderos.features.setting.domain.error.SettingError
import com.apptolast.invernaderos.features.setting.domain.model.Setting
import com.apptolast.invernaderos.features.setting.domain.port.input.UpdateSettingCommand
import com.apptolast.invernaderos.features.setting.domain.port.output.SettingRepositoryPort
import com.apptolast.invernaderos.features.setting.domain.port.output.SettingSectorValidationPort
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

class UpdateSettingUseCaseTest {

    private val repository = mockk<SettingRepositoryPort>()
    private val sectorValidation = mockk<SettingSectorValidationPort>()
    private val applicationEventPublisher = mockk<org.springframework.context.ApplicationEventPublisher>(relaxed = true)
    private val useCase = UpdateSettingUseCaseImpl(repository, sectorValidation, applicationEventPublisher)

    private val existing = Setting(
        id = SettingId(1L),
        code = "SET-00001",
        tenantId = TenantId(10L),
        sectorId = SectorId(20L),
        parameterId = 1,
        parameterName = "TEMPERATURE",
        actuatorStateId = 2,
        actuatorStateName = "ON",
        dataTypeId = 1,
        dataTypeName = "INTEGER",
        value = "25",
        description = "Temperatura maxima",
        clientName = null,
        isActive = true,
        createdAt = Instant.parse("2026-01-01T00:00:00Z"),
        updatedAt = Instant.parse("2026-01-01T00:00:00Z")
    )

    @Test
    fun `should update setting value`() {
        val command = UpdateSettingCommand(
            id = SettingId(1L),
            tenantId = TenantId(10L),
            value = "30"
        )
        every { repository.findByIdAndTenantId(SettingId(1L), TenantId(10L)) } returns existing
        every { repository.save(any()) } answers { firstArg() }

        val result = useCase.execute(command)

        assertThat(result).isInstanceOf(Either.Right::class.java)
        assertThat((result as Either.Right).value.value).isEqualTo("30")
    }

    @Test
    fun `should return NotFound when setting does not exist`() {
        val command = UpdateSettingCommand(id = SettingId(999L), tenantId = TenantId(10L))
        every { repository.findByIdAndTenantId(SettingId(999L), TenantId(10L)) } returns null

        val result = useCase.execute(command)

        assertThat(result).isInstanceOf(Either.Left::class.java)
        assertThat((result as Either.Left).value).isInstanceOf(SettingError.NotFound::class.java)
    }

    @Test
    fun `should return error when new sector does not belong to tenant`() {
        val command = UpdateSettingCommand(
            id = SettingId(1L),
            tenantId = TenantId(10L),
            sectorId = SectorId(99L)
        )
        every { repository.findByIdAndTenantId(SettingId(1L), TenantId(10L)) } returns existing
        every { sectorValidation.existsByIdAndTenantId(SectorId(99L), TenantId(10L)) } returns false

        val result = useCase.execute(command)

        assertThat(result).isInstanceOf(Either.Left::class.java)
        assertThat((result as Either.Left).value).isInstanceOf(SettingError.SectorNotOwnedByTenant::class.java)
        verify(exactly = 0) { repository.save(any()) }
    }
}
