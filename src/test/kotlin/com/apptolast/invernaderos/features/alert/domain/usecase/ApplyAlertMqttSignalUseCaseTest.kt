package com.apptolast.invernaderos.features.alert.domain.usecase

import com.apptolast.invernaderos.features.alert.application.usecase.ApplyAlertMqttSignalUseCaseImpl
import com.apptolast.invernaderos.features.alert.domain.error.AlertError
import com.apptolast.invernaderos.features.alert.domain.model.Alert
import com.apptolast.invernaderos.features.alert.domain.model.AlertMqttSignal
import com.apptolast.invernaderos.features.alert.domain.model.AlertSignalDecision
import com.apptolast.invernaderos.features.alert.domain.model.AlertSignalSource
import com.apptolast.invernaderos.features.alert.domain.model.AlertStateChange
import com.apptolast.invernaderos.features.alert.domain.port.output.AlertByCodeRepositoryPort
import com.apptolast.invernaderos.features.alert.domain.port.output.AlertSignalDecisionPort
import com.apptolast.invernaderos.features.alert.domain.port.output.AlertStateChangePersistencePort
import com.apptolast.invernaderos.features.alert.domain.port.output.AlertStateChangedEventPublisherPort
import com.apptolast.invernaderos.features.shared.domain.Either
import com.apptolast.invernaderos.features.shared.domain.model.SectorId
import com.apptolast.invernaderos.features.shared.domain.model.TenantId
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Instant

class ApplyAlertMqttSignalUseCaseTest {

    private val alertByCodeRepository = mockk<AlertByCodeRepositoryPort>()
    private val decisionPort = mockk<AlertSignalDecisionPort>()
    private val stateChangePort = mockk<AlertStateChangePersistencePort>()
    private val eventPublisher = mockk<AlertStateChangedEventPublisherPort>()

    private val useCase = ApplyAlertMqttSignalUseCaseImpl(
        alertByCodeRepository = alertByCodeRepository,
        decisionPort = decisionPort,
        stateChangePort = stateChangePort,
        eventPublisher = eventPublisher
    )

    // ---------------------------------------------------------------------------
    // Fixtures
    // ---------------------------------------------------------------------------

    private fun anUnresolvedAlert() = Alert(
        id = 100L,
        code = "ALT-00010",
        tenantId = TenantId(10L),
        sectorId = SectorId(20L),
        sectorCode = null,
        alertTypeId = null,
        alertTypeName = null,
        severityId = null,
        severityName = null,
        severityLevel = null,
        message = "Temperatura excede umbral",
        description = null,
        clientName = null,
        isResolved = false,
        resolvedAt = null,
        resolvedByUserId = null,
        resolvedByUserName = null,
        createdAt = Instant.parse("2026-01-01T00:00:00Z"),
        updatedAt = Instant.parse("2026-01-01T00:00:00Z")
    )

    private fun aResolvedAlert() = anUnresolvedAlert().copy(
        isResolved = true,
        resolvedAt = Instant.parse("2026-01-02T00:00:00Z"),
        resolvedByUserId = null
    )

    // ---------------------------------------------------------------------------
    // Test cases
    // ---------------------------------------------------------------------------

    @Test
    fun `should return UnknownCode when alert does not exist`() {
        every { alertByCodeRepository.findByCode("ALT-00010") } returns null

        val result = useCase.execute(AlertMqttSignal(code = "ALT-00010", rawValue = "1"))

        assertThat(result).isInstanceOf(Either.Left::class.java)
        assertThat((result as Either.Left).value).isInstanceOf(AlertError.UnknownCode::class.java)
        verify(exactly = 0) { decisionPort.decide(any(), any()) }
        verify(exactly = 0) { stateChangePort.save(any()) }
        verify(exactly = 0) { eventPublisher.publish(any(), any()) }
    }

    @Test
    fun `should return InvalidSignalValue when raw value is not parseable`() {
        every { alertByCodeRepository.findByCode("ALT-00010") } returns anUnresolvedAlert()

        val result = useCase.execute(AlertMqttSignal(code = "ALT-00010", rawValue = "banana"))

        assertThat(result).isInstanceOf(Either.Left::class.java)
        val error = (result as Either.Left).value
        assertThat(error).isInstanceOf(AlertError.InvalidSignalValue::class.java)
        val invalidError = error as AlertError.InvalidSignalValue
        assertThat(invalidError.code).isEqualTo("ALT-00010")
        assertThat(invalidError.rawValue).isEqualTo("banana")
        verify(exactly = 0) { decisionPort.decide(any(), any()) }
        verify(exactly = 0) { stateChangePort.save(any()) }
        verify(exactly = 0) { eventPublisher.publish(any(), any()) }
    }

    @Test
    fun `should accept rawValue 'true' as valid`() {
        val resolvedAlert = aResolvedAlert()
        every { alertByCodeRepository.findByCode("ALT-00010") } returns resolvedAlert
        every { decisionPort.decide(any(), any()) } returns AlertSignalDecision.ACTIVATE
        every { alertByCodeRepository.save(any()) } answers { firstArg() }
        every { stateChangePort.save(any()) } answers { firstArg() }
        justRun { eventPublisher.publish(any(), any()) }

        val result = useCase.execute(AlertMqttSignal(code = "ALT-00010", rawValue = "true"))

        assertThat(result).isInstanceOf(Either.Right::class.java)
        verify(exactly = 1) { alertByCodeRepository.save(any()) }
        verify(exactly = 1) { stateChangePort.save(any()) }
        verify(exactly = 1) { eventPublisher.publish(any(), any()) }
    }

    @Test
    fun `should accept rawValue '1' as valid`() {
        val resolvedAlert = aResolvedAlert()
        every { alertByCodeRepository.findByCode("ALT-00010") } returns resolvedAlert
        every { decisionPort.decide(any(), any()) } returns AlertSignalDecision.ACTIVATE
        every { alertByCodeRepository.save(any()) } answers { firstArg() }
        every { stateChangePort.save(any()) } answers { firstArg() }
        justRun { eventPublisher.publish(any(), any()) }

        val result = useCase.execute(AlertMqttSignal(code = "ALT-00010", rawValue = "1"))

        assertThat(result).isInstanceOf(Either.Right::class.java)
        verify(exactly = 1) { alertByCodeRepository.save(any()) }
    }

    @Test
    fun `should accept rawValue 'false' as valid`() {
        val unresolvedAlert = anUnresolvedAlert()
        every { alertByCodeRepository.findByCode("ALT-00010") } returns unresolvedAlert
        every { decisionPort.decide(any(), any()) } returns AlertSignalDecision.RESOLVE
        every { alertByCodeRepository.save(any()) } answers { firstArg() }
        every { stateChangePort.save(any()) } answers { firstArg() }
        justRun { eventPublisher.publish(any(), any()) }

        val result = useCase.execute(AlertMqttSignal(code = "ALT-00010", rawValue = "false"))

        assertThat(result).isInstanceOf(Either.Right::class.java)
        verify(exactly = 1) { alertByCodeRepository.save(any()) }
    }

    @Test
    fun `should accept rawValue '0' as valid`() {
        val unresolvedAlert = anUnresolvedAlert()
        every { alertByCodeRepository.findByCode("ALT-00010") } returns unresolvedAlert
        every { decisionPort.decide(any(), any()) } returns AlertSignalDecision.RESOLVE
        every { alertByCodeRepository.save(any()) } answers { firstArg() }
        every { stateChangePort.save(any()) } answers { firstArg() }
        justRun { eventPublisher.publish(any(), any()) }

        val result = useCase.execute(AlertMqttSignal(code = "ALT-00010", rawValue = "0"))

        assertThat(result).isInstanceOf(Either.Right::class.java)
        verify(exactly = 1) { alertByCodeRepository.save(any()) }
    }

    @Test
    fun `should be case-insensitive on rawValue`() {
        val resolvedAlert = aResolvedAlert()
        every { alertByCodeRepository.findByCode("ALT-00010") } returns resolvedAlert
        every { decisionPort.decide(any(), any()) } returns AlertSignalDecision.ACTIVATE
        every { alertByCodeRepository.save(any()) } answers { firstArg() }
        every { stateChangePort.save(any()) } answers { firstArg() }
        justRun { eventPublisher.publish(any(), any()) }

        val result = useCase.execute(AlertMqttSignal(code = "ALT-00010", rawValue = "TRUE"))

        assertThat(result).isInstanceOf(Either.Right::class.java)
        verify(exactly = 1) { alertByCodeRepository.save(any()) }
    }

    @Test
    fun `should return NoTransitionRequired when decision is NO_OP`() {
        val unresolvedAlert = anUnresolvedAlert()
        every { alertByCodeRepository.findByCode("ALT-00010") } returns unresolvedAlert
        every { decisionPort.decide(any(), any()) } returns AlertSignalDecision.NO_OP

        val result = useCase.execute(AlertMqttSignal(code = "ALT-00010", rawValue = "1"))

        assertThat(result).isInstanceOf(Either.Left::class.java)
        val error = (result as Either.Left).value
        assertThat(error).isInstanceOf(AlertError.NoTransitionRequired::class.java)
        verify(exactly = 0) { alertByCodeRepository.save(any()) }
        verify(exactly = 0) { stateChangePort.save(any()) }
        verify(exactly = 0) { eventPublisher.publish(any(), any()) }
    }

    @Test
    fun `should activate alert when decision is ACTIVATE`() {
        val resolvedAlert = aResolvedAlert()
        every { alertByCodeRepository.findByCode("ALT-00010") } returns resolvedAlert
        every { decisionPort.decide(any(), any()) } returns AlertSignalDecision.ACTIVATE
        every { alertByCodeRepository.save(any()) } answers { firstArg() }
        every { stateChangePort.save(any()) } answers { firstArg() }
        justRun { eventPublisher.publish(any(), any()) }

        val result = useCase.execute(AlertMqttSignal(code = "ALT-00010", rawValue = "1"))

        assertThat(result).isInstanceOf(Either.Right::class.java)
        val applied = (result as Either.Right).value

        // Alert transitions to active
        assertThat(applied.alert.isResolved).isFalse()
        assertThat(applied.alert.resolvedAt).isNull()
        assertThat(applied.alert.resolvedByUserId).isNull()

        // State change records the transition correctly
        val change = applied.change
        assertThat(change).isNotNull()
        assertThat(change!!.fromResolved).isTrue()
        assertThat(change.toResolved).isFalse()
        assertThat(change.source).isEqualTo(AlertSignalSource.MQTT)
        assertThat(change.rawValue).isEqualTo("1")

        verify(exactly = 1) { eventPublisher.publish(applied.alert, change) }
    }

    @Test
    fun `should resolve alert when decision is RESOLVE`() {
        val beforeNow = Instant.now()
        val unresolvedAlert = anUnresolvedAlert()
        every { alertByCodeRepository.findByCode("ALT-00010") } returns unresolvedAlert
        every { decisionPort.decide(any(), any()) } returns AlertSignalDecision.RESOLVE
        every { alertByCodeRepository.save(any()) } answers { firstArg() }
        every { stateChangePort.save(any()) } answers { firstArg() }
        justRun { eventPublisher.publish(any(), any()) }

        val result = useCase.execute(AlertMqttSignal(code = "ALT-00010", rawValue = "0"))

        assertThat(result).isInstanceOf(Either.Right::class.java)
        val applied = (result as Either.Right).value

        assertThat(applied.alert.isResolved).isTrue()
        assertThat(applied.alert.resolvedAt).isNotNull()
        assertThat(applied.alert.resolvedAt).isAfterOrEqualTo(beforeNow)
        assertThat(applied.alert.resolvedAt).isBeforeOrEqualTo(Instant.now())

        val change = applied.change
        assertThat(change).isNotNull()
        assertThat(change!!.fromResolved).isFalse()
        assertThat(change.toResolved).isTrue()
    }

    @Test
    fun `should set resolvedByUserId to null on RESOLVE since source is MQTT`() {
        val unresolvedAlert = anUnresolvedAlert()
        every { alertByCodeRepository.findByCode("ALT-00010") } returns unresolvedAlert
        every { decisionPort.decide(any(), any()) } returns AlertSignalDecision.RESOLVE
        every { alertByCodeRepository.save(any()) } answers { firstArg() }
        every { stateChangePort.save(any()) } answers { firstArg() }
        justRun { eventPublisher.publish(any(), any()) }

        val result = useCase.execute(AlertMqttSignal(code = "ALT-00010", rawValue = "0"))

        assertThat(result).isInstanceOf(Either.Right::class.java)
        val savedAlert = (result as Either.Right).value.alert
        assertThat(savedAlert.resolvedByUserId).isNull()
    }
}
