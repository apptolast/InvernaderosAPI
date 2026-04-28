package com.apptolast.invernaderos.features.alert.domain.usecase

import com.apptolast.invernaderos.features.alert.application.usecase.ResolveAlertUseCaseImpl
import com.apptolast.invernaderos.features.alert.domain.error.AlertError
import com.apptolast.invernaderos.features.alert.domain.model.Alert
import com.apptolast.invernaderos.features.alert.domain.model.AlertSignalSource
import com.apptolast.invernaderos.features.alert.domain.model.AlertStateChange
import com.apptolast.invernaderos.features.alert.domain.port.output.AlertRepositoryPort
import com.apptolast.invernaderos.features.alert.domain.port.output.AlertStateChangePersistencePort
import com.apptolast.invernaderos.features.alert.domain.port.output.AlertStateChangedEventPublisherPort
import com.apptolast.invernaderos.features.shared.domain.Either
import com.apptolast.invernaderos.features.shared.domain.model.SectorId
import com.apptolast.invernaderos.features.shared.domain.model.TenantId
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Instant

class ResolveAlertUseCaseTest {

    private val repository = mockk<AlertRepositoryPort>()
    private val stateChangePort = mockk<AlertStateChangePersistencePort>()
    private val eventPublisher = mockk<AlertStateChangedEventPublisherPort>()
    private val useCase = ResolveAlertUseCaseImpl(repository, stateChangePort, eventPublisher)

    private val unresolvedAlert = Alert(
        id = 1L, code = "ALT-00001", tenantId = TenantId(10L), sectorId = SectorId(20L),
        sectorCode = null, alertTypeId = null, alertTypeName = null,
        severityId = null, severityName = null, severityLevel = null,
        message = "Alerta activa", description = null,
        clientName = null,
        isResolved = false, resolvedAt = null, resolvedByUserId = null, resolvedByUserName = null,
        createdAt = Instant.parse("2026-01-01T00:00:00Z"), updatedAt = Instant.parse("2026-01-01T00:00:00Z")
    )

    private val resolvedAlert = unresolvedAlert.copy(
        isResolved = true,
        resolvedAt = Instant.parse("2026-01-02T00:00:00Z"),
        resolvedByUserId = 5L
    )

    @Test
    fun `should resolve alert when it is unresolved`() {
        every { repository.findByIdAndTenantId(1L, TenantId(10L)) } returns unresolvedAlert
        every { repository.save(any()) } answers {
            val a = firstArg<Alert>()
            a.copy(isResolved = true, resolvedAt = Instant.now(), resolvedByUserId = 5L)
        }
        every { stateChangePort.save(any()) } answers { firstArg<AlertStateChange>().copy(id = 99L) }
        justRun { eventPublisher.publish(any(), any()) }

        val result = useCase.resolve(1L, TenantId(10L), 5L)

        assertThat(result).isInstanceOf(Either.Right::class.java)
        assertThat((result as Either.Right).value.isResolved).isTrue()
        assertThat(result.value.resolvedByUserId).isEqualTo(5L)
        verify(exactly = 1) { repository.save(any()) }
        verify(exactly = 1) { stateChangePort.save(any()) }
        verify(exactly = 1) { eventPublisher.publish(any(), any()) }
    }

    @Test
    fun `should persist state change with source=API and rawValue=null on resolve`() {
        every { repository.findByIdAndTenantId(1L, TenantId(10L)) } returns unresolvedAlert
        every { repository.save(any()) } answers {
            firstArg<Alert>().copy(isResolved = true, resolvedAt = Instant.now(), resolvedByUserId = 5L)
        }
        val changeSlot = slot<AlertStateChange>()
        every { stateChangePort.save(capture(changeSlot)) } answers { firstArg<AlertStateChange>().copy(id = 99L) }
        justRun { eventPublisher.publish(any(), any()) }

        useCase.resolve(1L, TenantId(10L), 5L)

        assertThat(changeSlot.captured.source).isEqualTo(AlertSignalSource.API)
        assertThat(changeSlot.captured.rawValue).isNull()
        assertThat(changeSlot.captured.fromResolved).isFalse()
        assertThat(changeSlot.captured.toResolved).isTrue()
        assertThat(changeSlot.captured.alertId).isEqualTo(1L)
    }

    @Test
    fun `should return AlreadyResolved when alert is already resolved`() {
        every { repository.findByIdAndTenantId(1L, TenantId(10L)) } returns resolvedAlert

        val result = useCase.resolve(1L, TenantId(10L), null)

        assertThat(result).isInstanceOf(Either.Left::class.java)
        assertThat((result as Either.Left).value).isInstanceOf(AlertError.AlreadyResolved::class.java)
        verify(exactly = 0) { repository.save(any()) }
        verify(exactly = 0) { stateChangePort.save(any()) }
        verify(exactly = 0) { eventPublisher.publish(any(), any()) }
    }

    @Test
    fun `should reopen alert when it is resolved`() {
        every { repository.findByIdAndTenantId(1L, TenantId(10L)) } returns resolvedAlert
        every { repository.save(any()) } answers {
            firstArg<Alert>().copy(isResolved = false, resolvedAt = null, resolvedByUserId = null)
        }
        every { stateChangePort.save(any()) } answers { firstArg<AlertStateChange>().copy(id = 100L) }
        justRun { eventPublisher.publish(any(), any()) }

        val result = useCase.reopen(1L, TenantId(10L))

        assertThat(result).isInstanceOf(Either.Right::class.java)
        assertThat((result as Either.Right).value.isResolved).isFalse()
        assertThat(result.value.resolvedAt).isNull()
        verify(exactly = 1) { repository.save(any()) }
        verify(exactly = 1) { stateChangePort.save(any()) }
        verify(exactly = 1) { eventPublisher.publish(any(), any()) }
    }

    @Test
    fun `should persist state change with source=API and rawValue=null on reopen`() {
        every { repository.findByIdAndTenantId(1L, TenantId(10L)) } returns resolvedAlert
        every { repository.save(any()) } answers {
            firstArg<Alert>().copy(isResolved = false, resolvedAt = null, resolvedByUserId = null)
        }
        val changeSlot = slot<AlertStateChange>()
        every { stateChangePort.save(capture(changeSlot)) } answers { firstArg<AlertStateChange>().copy(id = 100L) }
        justRun { eventPublisher.publish(any(), any()) }

        useCase.reopen(1L, TenantId(10L))

        assertThat(changeSlot.captured.source).isEqualTo(AlertSignalSource.API)
        assertThat(changeSlot.captured.rawValue).isNull()
        assertThat(changeSlot.captured.fromResolved).isTrue()
        assertThat(changeSlot.captured.toResolved).isFalse()
    }

    @Test
    fun `should return NotResolved when alert is already open on reopen`() {
        every { repository.findByIdAndTenantId(1L, TenantId(10L)) } returns unresolvedAlert

        val result = useCase.reopen(1L, TenantId(10L))

        assertThat(result).isInstanceOf(Either.Left::class.java)
        assertThat((result as Either.Left).value).isInstanceOf(AlertError.NotResolved::class.java)
        verify(exactly = 0) { repository.save(any()) }
        verify(exactly = 0) { stateChangePort.save(any()) }
        verify(exactly = 0) { eventPublisher.publish(any(), any()) }
    }

    @Test
    fun `should return NotFound when alert does not exist on resolve`() {
        every { repository.findByIdAndTenantId(999L, TenantId(10L)) } returns null

        val result = useCase.resolve(999L, TenantId(10L), null)

        assertThat(result).isInstanceOf(Either.Left::class.java)
        assertThat((result as Either.Left).value).isInstanceOf(AlertError.NotFound::class.java)
        verify(exactly = 0) { stateChangePort.save(any()) }
        verify(exactly = 0) { eventPublisher.publish(any(), any()) }
    }

    @Test
    fun `should return NotFound when alert does not exist on reopen`() {
        every { repository.findByIdAndTenantId(999L, TenantId(10L)) } returns null

        val result = useCase.reopen(999L, TenantId(10L))

        assertThat(result).isInstanceOf(Either.Left::class.java)
        assertThat((result as Either.Left).value).isInstanceOf(AlertError.NotFound::class.java)
        verify(exactly = 0) { stateChangePort.save(any()) }
        verify(exactly = 0) { eventPublisher.publish(any(), any()) }
    }
}
