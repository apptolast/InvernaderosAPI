package com.apptolast.invernaderos.features.alert.infrastructure

import com.apptolast.invernaderos.features.alert.Alert
import com.apptolast.invernaderos.features.alert.AlertController
import com.apptolast.invernaderos.features.alert.AlertService
import com.apptolast.invernaderos.features.alert.domain.model.Alert as DomainAlert
import com.apptolast.invernaderos.features.alert.infrastructure.adapter.input.AlertRestInboundAdapter
import com.apptolast.invernaderos.features.shared.domain.Either
import com.apptolast.invernaderos.features.shared.domain.model.SectorId
import com.apptolast.invernaderos.features.shared.domain.model.TenantId
import com.apptolast.invernaderos.features.shared.security.TenantContext
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import java.time.Instant

class LegacyAlertControllerActorTest {

    private val alertService = mockk<AlertService>()
    private val restInboundAdapter = mockk<AlertRestInboundAdapter>()
    private val tenantContext = mockk<TenantContext>()
    private val controller = AlertController(alertService, restInboundAdapter, tenantContext)

    @Test
    fun `legacy resolve derives actor user id from authenticated context`() {
        val legacyAlert = sampleLegacyAlert(isResolved = false, resolvedByUserId = null)
        every { alertService.getById(1L) } returns legacyAlert
        every { tenantContext.currentTenantId() } returns 10L
        every { tenantContext.currentUserId() } returns 77L
        every { restInboundAdapter.resolve(1L, TenantId(10L), 77L) } returns
            Either.Right(sampleDomainAlert(isResolved = true, resolvedByUserId = 77L))

        val response = controller.resolveAlert(1L)

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        verify(exactly = 1) { restInboundAdapter.resolve(1L, TenantId(10L), 77L) }
    }

    @Test
    fun `legacy reopen derives actor user id from authenticated context`() {
        val legacyAlert = sampleLegacyAlert(isResolved = true, resolvedByUserId = 77L)
        every { alertService.getById(1L) } returns legacyAlert
        every { tenantContext.currentTenantId() } returns 10L
        every { tenantContext.currentUserId() } returns 77L
        every { restInboundAdapter.reopen(1L, TenantId(10L), 77L) } returns
            Either.Right(sampleDomainAlert(isResolved = false, resolvedByUserId = null))

        val response = controller.reopenAlert(1L)

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        verify(exactly = 1) { restInboundAdapter.reopen(1L, TenantId(10L), 77L) }
    }

    private fun sampleLegacyAlert(isResolved: Boolean, resolvedByUserId: Long?) = Alert(
        id = 1L,
        code = "ALT-00001",
        sectorId = 20L,
        tenantId = 10L,
        message = "Alert",
        isResolved = isResolved,
        resolvedAt = if (isResolved) Instant.parse("2026-01-01T00:00:00Z") else null,
        resolvedByUserId = resolvedByUserId,
        createdAt = Instant.parse("2026-01-01T00:00:00Z"),
        updatedAt = Instant.parse("2026-01-01T00:00:00Z"),
    )

    private fun sampleDomainAlert(isResolved: Boolean, resolvedByUserId: Long?) = DomainAlert(
        id = 1L,
        code = "ALT-00001",
        tenantId = TenantId(10L),
        sectorId = SectorId(20L),
        sectorCode = null,
        alertTypeId = null,
        alertTypeName = null,
        severityId = null,
        severityName = null,
        severityLevel = null,
        message = "Alert",
        description = null,
        clientName = null,
        isResolved = isResolved,
        resolvedAt = if (isResolved) Instant.parse("2026-01-01T00:00:00Z") else null,
        resolvedByUserId = resolvedByUserId,
        resolvedByUserName = null,
        createdAt = Instant.parse("2026-01-01T00:00:00Z"),
        updatedAt = Instant.parse("2026-01-01T00:00:00Z"),
    )
}
