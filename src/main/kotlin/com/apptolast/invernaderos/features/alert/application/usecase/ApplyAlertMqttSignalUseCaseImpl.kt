package com.apptolast.invernaderos.features.alert.application.usecase

import com.apptolast.invernaderos.features.alert.domain.error.AlertError
import com.apptolast.invernaderos.features.alert.domain.model.AlertActor
import com.apptolast.invernaderos.features.alert.domain.model.AlertMqttSignal
import com.apptolast.invernaderos.features.alert.domain.model.AlertSignalDecision
import com.apptolast.invernaderos.features.alert.domain.model.AlertSignalSource
import com.apptolast.invernaderos.features.alert.domain.model.AlertStateChange
import com.apptolast.invernaderos.features.alert.domain.port.input.AlertSignalApplied
import com.apptolast.invernaderos.features.alert.domain.port.input.ApplyAlertMqttSignalUseCase
import com.apptolast.invernaderos.features.alert.domain.port.output.AlertByCodeRepositoryPort
import com.apptolast.invernaderos.features.alert.domain.port.output.AlertSignalDecisionPort
import com.apptolast.invernaderos.features.alert.domain.port.output.AlertStateChangePersistencePort
import com.apptolast.invernaderos.features.alert.domain.port.output.AlertStateChangedEventPublisherPort
import com.apptolast.invernaderos.features.shared.domain.Either
import org.slf4j.LoggerFactory
import java.time.Instant

private val KNOWN_RAW_VALUES = setOf("true", "1", "false", "0")

/**
 * Use case implementation — plain Kotlin, no Spring annotations.
 * Bean wiring is done in AlertModuleConfig.
 *
 * Transaction boundary: the caller (AlertMqttInboundAdapter.handleSignal) is annotated
 * with @Transactional("metadataTransactionManager") so that the alert update and the
 * state change write happen atomically without requiring @Service on this class.
 */
class ApplyAlertMqttSignalUseCaseImpl(
    private val alertByCodeRepository: AlertByCodeRepositoryPort,
    private val decisionPort: AlertSignalDecisionPort,
    private val stateChangePort: AlertStateChangePersistencePort,
    private val eventPublisher: AlertStateChangedEventPublisherPort
) : ApplyAlertMqttSignalUseCase {

    private val logger = LoggerFactory.getLogger(javaClass)

    override fun execute(signal: AlertMqttSignal): Either<AlertError, AlertSignalApplied> {
        // 1. Strict mode: unknown code → reject
        val alert = alertByCodeRepository.findByCode(signal.code)
        if (alert == null) {
            logger.warn(
                "Unknown ALT code received via MQTT: {}, value={}, ignored (strict mode)",
                signal.code, signal.rawValue
            )
            return Either.Left(AlertError.UnknownCode(signal.code))
        }

        // 2. Validate raw value is a known boolean representation
        if (signal.rawValue.lowercase() !in KNOWN_RAW_VALUES) {
            logger.warn(
                "Unparseable MQTT value '{}' for alert code '{}' — cannot map to ACTIVATE/RESOLVE",
                signal.rawValue, signal.code
            )
            return Either.Left(AlertError.InvalidSignalValue(signal.code, signal.rawValue))
        }

        // 3. Compute decision
        val decision = decisionPort.decide(alert, signal)

        // 4. NO_OP → no transition needed
        if (decision == AlertSignalDecision.NO_OP) {
            logger.debug(
                "Alert {} signal ignored (no-op, already in target state, isResolved={})",
                alert.code, alert.isResolved
            )
            // Use -1L as a sentinel only if id is null (legacy data); operators should grep for `code` instead.
            return Either.Left(AlertError.NoTransitionRequired(alert.id ?: -1L, alert.isResolved))
        }

        // 5. Apply transition
        val targetResolved = decision == AlertSignalDecision.RESOLVE
        val updatedAlert = alert.copy(
            isResolved = targetResolved,
            resolvedAt = if (targetResolved) Instant.now() else null,
            resolvedByUserId = null, // source is MQTT — no user context
            updatedAt = Instant.now()
        )

        // 6. Persist alert (reload with EntityGraph via port)
        val persistedAlert = alertByCodeRepository.save(updatedAlert)

        // 7. Persist state change
        // actor_ref is null because we do not have device id available at this point in the MQTT pipeline.
        val change = AlertStateChange(
            id = null,
            alertId = persistedAlert.id ?: throw IllegalStateException("Alert ID cannot be null after save"),
            fromResolved = alert.isResolved,
            toResolved = targetResolved,
            source = AlertSignalSource.MQTT,
            rawValue = signal.rawValue,
            at = Instant.now(),
            actor = AlertActor.Device(deviceRef = null),
        )
        val persistedChange = stateChangePort.save(change)

        // 8. Publish Spring event for future WebSocket consumers
        eventPublisher.publish(persistedAlert, persistedChange)

        // Single observability log lives at the architectural boundary (AlertMqttInboundAdapter).
        return Either.Right(AlertSignalApplied(alert = persistedAlert, change = persistedChange))
    }
}
