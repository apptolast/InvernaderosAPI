package com.apptolast.invernaderos.features.alert.domain.usecase

import com.apptolast.invernaderos.features.alert.application.usecase.UpdateAlertUseCaseImpl
import com.apptolast.invernaderos.features.alert.domain.error.AlertError
import com.apptolast.invernaderos.features.alert.domain.model.Alert
import com.apptolast.invernaderos.features.alert.domain.port.input.UpdateAlertCommand
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

class UpdateAlertUseCaseTest {

    private val repository = mockk<AlertRepositoryPort>()
    private val sectorValidation = mockk<AlertSectorValidationPort>()
    private val useCase = UpdateAlertUseCaseImpl(repository, sectorValidation)

    private val existing = Alert(
        id = 1L, code = "ALT-00001", tenantId = TenantId(10L), sectorId = SectorId(20L),
        sectorCode = "SEC-00001", alertTypeId = 1, alertTypeName = "THRESHOLD_EXCEEDED",
        severityId = 2, severityName = "WARNING", severityLevel = 2,
        message = "Temperatura alta", description = null,
        clientName = null,
        isResolved = false, resolvedAt = null, resolvedByUserId = null, resolvedByUserName = null,
        createdAt = Instant.parse("2026-01-01T00:00:00Z"), updatedAt = Instant.parse("2026-01-01T00:00:00Z")
    )

    @Test
    fun `should update alert message`() {
        val command = UpdateAlertCommand(id = 1L, tenantId = TenantId(10L), message = "Temperatura critica")
        every { repository.findByIdAndTenantId(1L, TenantId(10L)) } returns existing
        every { repository.save(any()) } answers { firstArg() }

        val result = useCase.execute(command)

        assertThat(result).isInstanceOf(Either.Right::class.java)
        assertThat((result as Either.Right).value.message).isEqualTo("Temperatura critica")
    }

    @Test
    fun `should return NotFound when alert does not exist`() {
        val command = UpdateAlertCommand(id = 999L, tenantId = TenantId(10L))
        every { repository.findByIdAndTenantId(999L, TenantId(10L)) } returns null

        val result = useCase.execute(command)

        assertThat(result).isInstanceOf(Either.Left::class.java)
        assertThat((result as Either.Left).value).isInstanceOf(AlertError.NotFound::class.java)
    }

    @Test
    fun `should return error when new sector does not belong to tenant`() {
        val command = UpdateAlertCommand(id = 1L, tenantId = TenantId(10L), sectorId = SectorId(99L))
        every { repository.findByIdAndTenantId(1L, TenantId(10L)) } returns existing
        every { sectorValidation.existsByIdAndTenantId(SectorId(99L), TenantId(10L)) } returns false

        val result = useCase.execute(command)

        assertThat(result).isInstanceOf(Either.Left::class.java)
        assertThat((result as Either.Left).value).isInstanceOf(AlertError.SectorNotOwnedByTenant::class.java)
        verify(exactly = 0) { repository.save(any()) }
    }
}
