package com.apptolast.invernaderos.features.setting.domain.usecase

import com.apptolast.invernaderos.features.setting.application.usecase.CreateSettingUseCaseImpl
import com.apptolast.invernaderos.features.setting.domain.error.SettingError
import com.apptolast.invernaderos.features.setting.domain.model.Setting
import com.apptolast.invernaderos.features.setting.domain.port.input.CreateSettingCommand
import com.apptolast.invernaderos.features.setting.domain.port.output.SettingCodeGenerator
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

class CreateSettingUseCaseTest {

    private val repository = mockk<SettingRepositoryPort>()
    private val codeGenerator = mockk<SettingCodeGenerator>()
    private val sectorValidation = mockk<SettingSectorValidationPort>()
    private val useCase = CreateSettingUseCaseImpl(repository, codeGenerator, sectorValidation)

    @Test
    fun `should create setting when sector belongs to tenant`() {
        val command = CreateSettingCommand(
            tenantId = TenantId(1L),
            sectorId = SectorId(10L),
            parameterId = 1,
            actuatorStateId = 2,
            dataTypeId = 1,
            value = "25",
            description = "Temperatura maxima",
            isActive = true
        )

        every { sectorValidation.existsByIdAndTenantId(SectorId(10L), TenantId(1L)) } returns true
        every { codeGenerator.generate() } returns "SET-00001"
        every { repository.save(any()) } answers {
            val s = firstArg<Setting>()
            s.copy(id = SettingId(100L), parameterName = "TEMPERATURE", actuatorStateName = "ON", dataTypeName = "INTEGER")
        }

        val result = useCase.execute(command)

        assertThat(result).isInstanceOf(Either.Right::class.java)
        val setting = (result as Either.Right).value
        assertThat(setting.id).isEqualTo(SettingId(100L))
        assertThat(setting.code).isEqualTo("SET-00001")
        assertThat(setting.parameterId).isEqualTo(1.toShort())
        verify(exactly = 1) { repository.save(any()) }
    }

    @Test
    fun `should return error when sector does not belong to tenant`() {
        val command = CreateSettingCommand(
            tenantId = TenantId(1L),
            sectorId = SectorId(99L),
            parameterId = 1
        )

        every { sectorValidation.existsByIdAndTenantId(SectorId(99L), TenantId(1L)) } returns false

        val result = useCase.execute(command)

        assertThat(result).isInstanceOf(Either.Left::class.java)
        assertThat((result as Either.Left).value).isInstanceOf(SettingError.SectorNotOwnedByTenant::class.java)
        verify(exactly = 0) { repository.save(any()) }
    }
}
