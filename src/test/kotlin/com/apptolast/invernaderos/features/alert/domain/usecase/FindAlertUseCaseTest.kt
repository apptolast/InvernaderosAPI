package com.apptolast.invernaderos.features.alert.domain.usecase

import com.apptolast.invernaderos.features.alert.application.usecase.FindAlertUseCaseImpl
import com.apptolast.invernaderos.features.alert.domain.error.AlertError
import com.apptolast.invernaderos.features.alert.domain.model.Alert
import com.apptolast.invernaderos.features.alert.domain.port.output.AlertRepositoryPort
import com.apptolast.invernaderos.features.shared.domain.Either
import com.apptolast.invernaderos.features.shared.domain.model.SectorId
import com.apptolast.invernaderos.features.shared.domain.model.TenantId
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Instant

class FindAlertUseCaseTest {

    private val repository = mockk<AlertRepositoryPort>()
    private val useCase = FindAlertUseCaseImpl(repository)

    private val sample = Alert(
        id = 1L, code = "ALT-00001", tenantId = TenantId(10L), sectorId = SectorId(20L),
        sectorCode = "SEC-00001", alertTypeId = 1, alertTypeName = "THRESHOLD_EXCEEDED",
        severityId = 2, severityName = "WARNING", severityLevel = 2,
        message = "Temperatura alta", description = null,
        clientName = null,
        isResolved = false, resolvedAt = null, resolvedByUserId = null, resolvedByUserName = null,
        createdAt = Instant.now(), updatedAt = Instant.now()
    )

    @Test
    fun `should find alert by id and tenant`() {
        every { repository.findByIdAndTenantId(1L, TenantId(10L)) } returns sample

        val result = useCase.findByIdAndTenantId(1L, TenantId(10L))

        assertThat(result).isInstanceOf(Either.Right::class.java)
        assertThat((result as Either.Right).value.code).isEqualTo("ALT-00001")
    }

    @Test
    fun `should return NotFound when alert does not exist`() {
        every { repository.findByIdAndTenantId(999L, TenantId(10L)) } returns null

        val result = useCase.findByIdAndTenantId(999L, TenantId(10L))

        assertThat(result).isInstanceOf(Either.Left::class.java)
        assertThat((result as Either.Left).value).isInstanceOf(AlertError.NotFound::class.java)
    }

    @Test
    fun `should find all alerts by tenant`() {
        every { repository.findAllByTenantId(TenantId(10L)) } returns listOf(sample)

        assertThat(useCase.findAllByTenantId(TenantId(10L))).hasSize(1)
    }
}
