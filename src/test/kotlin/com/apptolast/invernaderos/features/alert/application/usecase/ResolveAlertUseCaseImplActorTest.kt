package com.apptolast.invernaderos.features.alert.application.usecase

import com.apptolast.invernaderos.features.alert.domain.model.Alert
import com.apptolast.invernaderos.features.alert.domain.model.AlertActor
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
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Instant

/**
 * Unit tests focused on the actor-writing extension in [ResolveAlertUseCaseImpl].
 * Verifies that the correct [AlertActor] is captured in the persisted [AlertStateChange]
 * for both resolve and reopen flows, with and without a userId.
 */
class ResolveAlertUseCaseImplActorTest {

    private val repository = mockk<AlertRepositoryPort>()
    private val stateChangePort = mockk<AlertStateChangePersistencePort>()
    private val eventPublisher = mockk<AlertStateChangedEventPublisherPort>()
    private val useCase = ResolveAlertUseCaseImpl(repository, stateChangePort, eventPublisher)

    private val tenantId = TenantId(10L)

    private val unresolvedAlert = Alert(
        id = 1L,
        code = "ALT-00001",
        tenantId = tenantId,
        sectorId = SectorId(20L),
        sectorCode = null,
        alertTypeId = null,
        alertTypeName = null,
        severityId = null,
        severityName = null,
        severityLevel = null,
        message = "Active alert",
        description = null,
        clientName = null,
        isResolved = false,
        resolvedAt = null,
        resolvedByUserId = null,
        resolvedByUserName = null,
        createdAt = Instant.parse("2026-01-01T00:00:00Z"),
        updatedAt = Instant.parse("2026-01-01T00:00:00Z"),
    )

    private val resolvedAlert = unresolvedAlert.copy(
        isResolved = true,
        resolvedAt = Instant.parse("2026-01-01T06:00:00Z"),
        resolvedByUserId = 42L,
    )

    // -----------------------------------------------------------------------
    // resolve() actor tests
    // -----------------------------------------------------------------------

    @Test
    fun `resolve with userId should persist AlertActor dot User with correct userId`() {
        every { repository.findByIdAndTenantId(1L, tenantId) } returns unresolvedAlert
        every { repository.save(any()) } answers { firstArg<Alert>().copy(id = 1L, isResolved = true) }
        val changeSlot = slot<AlertStateChange>()
        every { stateChangePort.save(capture(changeSlot)) } answers { firstArg<AlertStateChange>().copy(id = 99L) }
        justRun { eventPublisher.publish(any(), any()) }

        val result = useCase.resolve(1L, tenantId, resolvedByUserId = 42L)

        assertThat(result).isInstanceOf(Either.Right::class.java)
        val actor = changeSlot.captured.actor
        assertThat(actor).isInstanceOf(AlertActor.User::class.java)
        assertThat((actor as AlertActor.User).userId).isEqualTo(42L)
    }

    @Test
    fun `resolve with userId should set source to API`() {
        every { repository.findByIdAndTenantId(1L, tenantId) } returns unresolvedAlert
        every { repository.save(any()) } answers { firstArg<Alert>().copy(id = 1L, isResolved = true) }
        val changeSlot = slot<AlertStateChange>()
        every { stateChangePort.save(capture(changeSlot)) } answers { firstArg<AlertStateChange>().copy(id = 99L) }
        justRun { eventPublisher.publish(any(), any()) }

        useCase.resolve(1L, tenantId, resolvedByUserId = 42L)

        assertThat(changeSlot.captured.source).isEqualTo(AlertSignalSource.API)
    }

    @Test
    fun `resolve with userId should set toResolved=true and fromResolved=false`() {
        every { repository.findByIdAndTenantId(1L, tenantId) } returns unresolvedAlert
        every { repository.save(any()) } answers { firstArg<Alert>().copy(id = 1L, isResolved = true) }
        val changeSlot = slot<AlertStateChange>()
        every { stateChangePort.save(capture(changeSlot)) } answers { firstArg<AlertStateChange>().copy(id = 99L) }
        justRun { eventPublisher.publish(any(), any()) }

        useCase.resolve(1L, tenantId, resolvedByUserId = 42L)

        assertThat(changeSlot.captured.toResolved).isTrue()
        assertThat(changeSlot.captured.fromResolved).isFalse()
    }

    @Test
    fun `resolve without userId should persist AlertActor dot System`() {
        every { repository.findByIdAndTenantId(1L, tenantId) } returns unresolvedAlert
        every { repository.save(any()) } answers { firstArg<Alert>().copy(id = 1L, isResolved = true) }
        val changeSlot = slot<AlertStateChange>()
        every { stateChangePort.save(capture(changeSlot)) } answers { firstArg<AlertStateChange>().copy(id = 99L) }
        justRun { eventPublisher.publish(any(), any()) }

        useCase.resolve(1L, tenantId, resolvedByUserId = null)

        assertThat(changeSlot.captured.actor).isEqualTo(AlertActor.System)
    }

    @Test
    fun `resolve with different userId values should each map to AlertActor dot User with correct userId`() {
        listOf(1L, 100L, Long.MAX_VALUE).forEach { userId ->
            every { repository.findByIdAndTenantId(1L, tenantId) } returns unresolvedAlert
            every { repository.save(any()) } answers { firstArg<Alert>().copy(id = 1L, isResolved = true) }
            val changeSlot = slot<AlertStateChange>()
            every { stateChangePort.save(capture(changeSlot)) } answers { firstArg<AlertStateChange>().copy(id = 99L) }
            justRun { eventPublisher.publish(any(), any()) }

            useCase.resolve(1L, tenantId, resolvedByUserId = userId)

            val actor = changeSlot.captured.actor
            assertThat(actor).isInstanceOf(AlertActor.User::class.java)
            assertThat((actor as AlertActor.User).userId).isEqualTo(userId)
        }
    }

    // -----------------------------------------------------------------------
    // reopen() actor tests
    // -----------------------------------------------------------------------

    @Test
    fun `reopen with actorUserId should persist AlertActor dot User with correct userId`() {
        every { repository.findByIdAndTenantId(1L, tenantId) } returns resolvedAlert
        every { repository.save(any()) } answers {
            firstArg<Alert>().copy(id = 1L, isResolved = false, resolvedAt = null, resolvedByUserId = null)
        }
        val changeSlot = slot<AlertStateChange>()
        every { stateChangePort.save(capture(changeSlot)) } answers { firstArg<AlertStateChange>().copy(id = 100L) }
        justRun { eventPublisher.publish(any(), any()) }

        val result = useCase.reopen(1L, tenantId, actorUserId = 42L)

        assertThat(result).isInstanceOf(Either.Right::class.java)
        val actor = changeSlot.captured.actor
        assertThat(actor).isInstanceOf(AlertActor.User::class.java)
        assertThat((actor as AlertActor.User).userId).isEqualTo(42L)
    }

    @Test
    fun `reopen with actorUserId should set source to API`() {
        every { repository.findByIdAndTenantId(1L, tenantId) } returns resolvedAlert
        every { repository.save(any()) } answers {
            firstArg<Alert>().copy(id = 1L, isResolved = false, resolvedAt = null, resolvedByUserId = null)
        }
        val changeSlot = slot<AlertStateChange>()
        every { stateChangePort.save(capture(changeSlot)) } answers { firstArg<AlertStateChange>().copy(id = 100L) }
        justRun { eventPublisher.publish(any(), any()) }

        useCase.reopen(1L, tenantId, actorUserId = 42L)

        assertThat(changeSlot.captured.source).isEqualTo(AlertSignalSource.API)
    }

    @Test
    fun `reopen with actorUserId should set fromResolved=true and toResolved=false`() {
        every { repository.findByIdAndTenantId(1L, tenantId) } returns resolvedAlert
        every { repository.save(any()) } answers {
            firstArg<Alert>().copy(id = 1L, isResolved = false, resolvedAt = null, resolvedByUserId = null)
        }
        val changeSlot = slot<AlertStateChange>()
        every { stateChangePort.save(capture(changeSlot)) } answers { firstArg<AlertStateChange>().copy(id = 100L) }
        justRun { eventPublisher.publish(any(), any()) }

        useCase.reopen(1L, tenantId, actorUserId = 42L)

        assertThat(changeSlot.captured.fromResolved).isTrue()
        assertThat(changeSlot.captured.toResolved).isFalse()
    }

    @Test
    fun `reopen without actorUserId should persist AlertActor dot System`() {
        every { repository.findByIdAndTenantId(1L, tenantId) } returns resolvedAlert
        every { repository.save(any()) } answers {
            firstArg<Alert>().copy(id = 1L, isResolved = false, resolvedAt = null, resolvedByUserId = null)
        }
        val changeSlot = slot<AlertStateChange>()
        every { stateChangePort.save(capture(changeSlot)) } answers { firstArg<AlertStateChange>().copy(id = 100L) }
        justRun { eventPublisher.publish(any(), any()) }

        useCase.reopen(1L, tenantId, actorUserId = null)

        assertThat(changeSlot.captured.actor).isEqualTo(AlertActor.System)
    }

    @Test
    fun `reopen with default actorUserId omitted should persist AlertActor dot System`() {
        every { repository.findByIdAndTenantId(1L, tenantId) } returns resolvedAlert
        every { repository.save(any()) } answers {
            firstArg<Alert>().copy(id = 1L, isResolved = false, resolvedAt = null, resolvedByUserId = null)
        }
        val changeSlot = slot<AlertStateChange>()
        every { stateChangePort.save(capture(changeSlot)) } answers { firstArg<AlertStateChange>().copy(id = 100L) }
        justRun { eventPublisher.publish(any(), any()) }

        // Call with default actorUserId (null)
        useCase.reopen(1L, tenantId)

        assertThat(changeSlot.captured.actor).isEqualTo(AlertActor.System)
    }

    @Test
    fun `resolve should not populate username or displayName (those are hydrated at query time)`() {
        every { repository.findByIdAndTenantId(1L, tenantId) } returns unresolvedAlert
        every { repository.save(any()) } answers { firstArg<Alert>().copy(id = 1L, isResolved = true) }
        val changeSlot = slot<AlertStateChange>()
        every { stateChangePort.save(capture(changeSlot)) } answers { firstArg<AlertStateChange>().copy(id = 99L) }
        justRun { eventPublisher.publish(any(), any()) }

        useCase.resolve(1L, tenantId, resolvedByUserId = 42L)

        val actor = changeSlot.captured.actor as AlertActor.User
        assertThat(actor.username).isNull()
        assertThat(actor.displayName).isNull()
    }
}
