package com.apptolast.invernaderos.features.setting.domain.usecase

import com.apptolast.invernaderos.features.setting.application.usecase.FindSettingUseCaseImpl
import com.apptolast.invernaderos.features.setting.domain.error.SettingError
import com.apptolast.invernaderos.features.setting.domain.model.Setting
import com.apptolast.invernaderos.features.setting.domain.port.output.SettingRepositoryPort
import com.apptolast.invernaderos.features.shared.domain.Either
import com.apptolast.invernaderos.features.shared.domain.model.SectorId
import com.apptolast.invernaderos.features.shared.domain.model.SettingId
import com.apptolast.invernaderos.features.shared.domain.model.TenantId
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Instant

class FindSettingUseCaseTest {

    private val repository = mockk<SettingRepositoryPort>()
    private val useCase = FindSettingUseCaseImpl(repository)

    private val sample = Setting(
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
        createdAt = Instant.now(),
        updatedAt = Instant.now()
    )

    @Test
    fun `should find setting by id and tenant`() {
        every { repository.findByIdAndTenantId(SettingId(1L), TenantId(10L)) } returns sample

        val result = useCase.findByIdAndTenantId(SettingId(1L), TenantId(10L))

        assertThat(result).isInstanceOf(Either.Right::class.java)
        assertThat((result as Either.Right).value.code).isEqualTo("SET-00001")
    }

    @Test
    fun `should return NotFound when setting does not exist`() {
        every { repository.findByIdAndTenantId(SettingId(999L), TenantId(10L)) } returns null

        val result = useCase.findByIdAndTenantId(SettingId(999L), TenantId(10L))

        assertThat(result).isInstanceOf(Either.Left::class.java)
        assertThat((result as Either.Left).value).isInstanceOf(SettingError.NotFound::class.java)
    }

    @Test
    fun `should find all settings by tenant`() {
        every { repository.findAllByTenantId(TenantId(10L)) } returns listOf(sample)

        assertThat(useCase.findAllByTenantId(TenantId(10L))).hasSize(1)
    }

    @Test
    fun `should find all settings by sector`() {
        every { repository.findAllBySectorId(SectorId(20L)) } returns listOf(sample)

        assertThat(useCase.findAllBySectorId(SectorId(20L))).hasSize(1)
    }

    @Test
    fun `should find active settings by sector`() {
        every { repository.findActiveBySectorId(SectorId(20L)) } returns listOf(sample)

        assertThat(useCase.findActiveBySectorId(SectorId(20L))).hasSize(1)
    }
}
