package com.apptolast.invernaderos.features.alert.domain.usecase

import com.apptolast.invernaderos.features.alert.application.usecase.DeleteAlertUseCaseImpl
import com.apptolast.invernaderos.features.alert.domain.error.AlertError
import com.apptolast.invernaderos.features.alert.domain.model.Alert
import com.apptolast.invernaderos.features.alert.domain.port.output.AlertRepositoryPort
import com.apptolast.invernaderos.features.shared.domain.Either
import com.apptolast.invernaderos.features.shared.domain.model.SectorId
import com.apptolast.invernaderos.features.shared.domain.model.TenantId
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Instant

class DeleteAlertUseCaseTest {

    private val repository = mockk<AlertRepositoryPort>()
    private val useCase = DeleteAlertUseCaseImpl(repository)

    private val existing = Alert(
        id = 1L, code = "ALT-00001", tenantId = TenantId(10L), sectorId = SectorId(20L),
        sectorCode = null, alertTypeId = null, alertTypeName = null,
        severityId = null, severityName = null, severityLevel = null,
        message = "Alerta test", description = null,
        isResolved = false, resolvedAt = null, resolvedByUserId = null, resolvedByUserName = null,
        createdAt = Instant.now(), updatedAt = Instant.now()
    )

    @Test
    fun `should delete alert when it exists`() {
        every { repository.findByIdAndTenantId(1L, TenantId(10L)) } returns existing
        every { repository.delete(1L, TenantId(10L)) } returns true

        val result = useCase.execute(1L, TenantId(10L))

        assertThat(result).isInstanceOf(Either.Right::class.java)
        verify(exactly = 1) { repository.delete(1L, TenantId(10L)) }
    }

    @Test
    fun `should return NotFound when alert does not exist`() {
        every { repository.findByIdAndTenantId(999L, TenantId(10L)) } returns null

        val result = useCase.execute(999L, TenantId(10L))

        assertThat(result).isInstanceOf(Either.Left::class.java)
        assertThat((result as Either.Left).value).isInstanceOf(AlertError.NotFound::class.java)
        verify(exactly = 0) { repository.delete(any(), any()) }
    }
}
