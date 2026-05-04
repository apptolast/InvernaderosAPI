package com.apptolast.invernaderos.features.alert.infrastructure

import com.apptolast.invernaderos.features.alert.domain.model.Alert
import com.apptolast.invernaderos.features.alert.domain.port.input.CreateAlertUseCase
import com.apptolast.invernaderos.features.alert.domain.port.input.DeleteAlertUseCase
import com.apptolast.invernaderos.features.alert.domain.port.input.FindAlertUseCase
import com.apptolast.invernaderos.features.alert.domain.port.input.UpdateAlertUseCase
import com.apptolast.invernaderos.features.alert.infrastructure.adapter.input.AlertRestInboundAdapter
import com.apptolast.invernaderos.features.alert.infrastructure.adapter.input.TenantAlertController
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

class TenantAlertControllerActorTest {

    private val restInboundAdapter = mockk<AlertRestInboundAdapter>()
    private val tenantContext = mockk<TenantContext>()
    private val controller = TenantAlertController(
        createUseCase = mockk<CreateAlertUseCase>(),
        findUseCase = mockk<FindAlertUseCase>(),
        updateUseCase = mockk<UpdateAlertUseCase>(),
        deleteUseCase = mockk<DeleteAlertUseCase>(),
        restInboundAdapter = restInboundAdapter,
        tenantContext = tenantContext,
    )

    @Test
    fun `resolve derives actor user id from authenticated context`() {
        every { tenantContext.currentUserId() } returns 77L
        every { restInboundAdapter.resolve(1L, TenantId(10L), 77L) } returns
            Either.Right(sampleAlert(isResolved = true, resolvedByUserId = 77L))

        val response = controller.resolve(10L, 1L)

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        verify(exactly = 1) { restInboundAdapter.resolve(1L, TenantId(10L), 77L) }
    }

    @Test
    fun `reopen derives actor user id from authenticated context`() {
        every { tenantContext.currentUserId() } returns 77L
        every { restInboundAdapter.reopen(1L, TenantId(10L), 77L) } returns
            Either.Right(sampleAlert(isResolved = false, resolvedByUserId = null))

        val response = controller.reopen(10L, 1L)

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        verify(exactly = 1) { restInboundAdapter.reopen(1L, TenantId(10L), 77L) }
    }

    private fun sampleAlert(isResolved: Boolean, resolvedByUserId: Long?) = Alert(
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
