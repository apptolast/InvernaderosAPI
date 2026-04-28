package com.apptolast.invernaderos.features.alert.infrastructure

import com.apptolast.invernaderos.features.alert.domain.error.AlertError
import com.apptolast.invernaderos.features.alert.domain.model.Alert
import com.apptolast.invernaderos.features.alert.domain.port.input.ResolveAlertUseCase
import com.apptolast.invernaderos.features.alert.infrastructure.adapter.input.AlertRestInboundAdapter
import com.apptolast.invernaderos.features.shared.domain.Either
import com.apptolast.invernaderos.features.shared.domain.model.SectorId
import com.apptolast.invernaderos.features.shared.domain.model.TenantId
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Instant

/**
 * Unit test verifying that AlertRestInboundAdapter is a thin pass-through
 * to the underlying ResolveAlertUseCase. The actual @Transactional behavior
 * is verified end-to-end via @SpringBootTest (out of scope for this unit test).
 */
class AlertRestInboundAdapterTest {

    private val resolveUseCase = mockk<ResolveAlertUseCase>()
    private val adapter = AlertRestInboundAdapter(resolveUseCase)

    private val sampleAlert = Alert(
        id = 1L, code = "ALT-00001", tenantId = TenantId(10L), sectorId = SectorId(20L),
        sectorCode = null, alertTypeId = null, alertTypeName = null,
        severityId = null, severityName = null, severityLevel = null,
        message = null, description = null, clientName = null,
        isResolved = true, resolvedAt = Instant.now(), resolvedByUserId = 5L, resolvedByUserName = null,
        createdAt = Instant.parse("2026-01-01T00:00:00Z"),
        updatedAt = Instant.parse("2026-01-01T00:00:00Z")
    )

    @Test
    fun `resolve delegates to use case with same parameters`() {
        every { resolveUseCase.resolve(1L, TenantId(10L), 5L) } returns Either.Right(sampleAlert)

        val result = adapter.resolve(1L, TenantId(10L), 5L)

        assertThat(result).isInstanceOf(Either.Right::class.java)
        verify(exactly = 1) { resolveUseCase.resolve(1L, TenantId(10L), 5L) }
    }

    @Test
    fun `resolve propagates Left from use case unchanged`() {
        every { resolveUseCase.resolve(any(), any(), any()) } returns Either.Left(AlertError.AlreadyResolved(1L))

        val result = adapter.resolve(1L, TenantId(10L), null)

        assertThat(result).isInstanceOf(Either.Left::class.java)
        assertThat((result as Either.Left).value).isInstanceOf(AlertError.AlreadyResolved::class.java)
    }

    @Test
    fun `reopen delegates to use case with same parameters`() {
        every { resolveUseCase.reopen(1L, TenantId(10L)) } returns Either.Right(sampleAlert.copy(isResolved = false, resolvedAt = null))

        val result = adapter.reopen(1L, TenantId(10L))

        assertThat(result).isInstanceOf(Either.Right::class.java)
        verify(exactly = 1) { resolveUseCase.reopen(1L, TenantId(10L)) }
    }

    @Test
    fun `reopen propagates NotResolved Left unchanged`() {
        every { resolveUseCase.reopen(any(), any()) } returns Either.Left(AlertError.NotResolved(1L))

        val result = adapter.reopen(1L, TenantId(10L))

        assertThat(result).isInstanceOf(Either.Left::class.java)
        assertThat((result as Either.Left).value).isInstanceOf(AlertError.NotResolved::class.java)
    }
}
