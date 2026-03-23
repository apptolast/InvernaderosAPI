package com.apptolast.invernaderos.features.alert.domain.usecase

import com.apptolast.invernaderos.features.alert.application.usecase.CreateAlertUseCaseImpl
import com.apptolast.invernaderos.features.alert.domain.error.AlertError
import com.apptolast.invernaderos.features.alert.domain.model.Alert
import com.apptolast.invernaderos.features.alert.domain.port.input.CreateAlertCommand
import com.apptolast.invernaderos.features.alert.domain.port.output.AlertCodeGenerator
import com.apptolast.invernaderos.features.alert.domain.port.output.AlertRepositoryPort
import com.apptolast.invernaderos.features.alert.domain.port.output.AlertSectorValidationPort
import com.apptolast.invernaderos.features.shared.domain.Either
import com.apptolast.invernaderos.features.shared.domain.model.SectorId
import com.apptolast.invernaderos.features.shared.domain.model.TenantId
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Instant

class CreateAlertUseCaseTest {

    private val repository = mockk<AlertRepositoryPort>()
    private val codeGenerator = mockk<AlertCodeGenerator>()
    private val sectorValidation = mockk<AlertSectorValidationPort>()
    private val useCase = CreateAlertUseCaseImpl(repository, codeGenerator, sectorValidation)

    @Test
    fun `should create alert when sector belongs to tenant`() {
        val command = CreateAlertCommand(
            tenantId = TenantId(1L),
            sectorId = SectorId(10L),
            alertTypeId = 1,
            severityId = 2,
            message = "Temperatura excede umbral",
            description = "Detalle de la alerta"
        )

        every { sectorValidation.existsByIdAndTenantId(SectorId(10L), TenantId(1L)) } returns true
        every { codeGenerator.generate() } returns "ALT-00001"
        every { repository.save(any()) } answers {
            val a = firstArg<Alert>()
            a.copy(id = 100L, sectorCode = "SEC-00001", alertTypeName = "THRESHOLD_EXCEEDED", severityName = "WARNING", severityLevel = 2)
        }

        val result = useCase.execute(command)

        assertThat(result).isInstanceOf(Either.Right::class.java)
        val alert = (result as Either.Right).value
        assertThat(alert.id).isEqualTo(100L)
        assertThat(alert.code).isEqualTo("ALT-00001")
        assertThat(alert.message).isEqualTo("Temperatura excede umbral")
        assertThat(alert.isResolved).isFalse()
        verify(exactly = 1) { repository.save(any()) }
    }

    @Test
    fun `should return error when sector does not belong to tenant`() {
        val command = CreateAlertCommand(
            tenantId = TenantId(1L),
            sectorId = SectorId(99L),
            message = "Alerta test"
        )

        every { sectorValidation.existsByIdAndTenantId(SectorId(99L), TenantId(1L)) } returns false

        val result = useCase.execute(command)

        assertThat(result).isInstanceOf(Either.Left::class.java)
        assertThat((result as Either.Left).value).isInstanceOf(AlertError.SectorNotOwnedByTenant::class.java)
        verify(exactly = 0) { repository.save(any()) }
    }
}
