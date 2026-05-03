package com.apptolast.invernaderos.features.alert.application.usecase

import com.apptolast.invernaderos.features.alert.domain.error.AlertError
import com.apptolast.invernaderos.features.alert.domain.model.Alert
import com.apptolast.invernaderos.features.alert.domain.model.AlertActor
import com.apptolast.invernaderos.features.alert.domain.model.AlertSignalSource
import com.apptolast.invernaderos.features.alert.domain.model.AlertStateChange
import com.apptolast.invernaderos.features.alert.domain.port.input.ResolveAlertUseCase
import com.apptolast.invernaderos.features.alert.domain.port.output.AlertRepositoryPort
import com.apptolast.invernaderos.features.alert.domain.port.output.AlertStateChangePersistencePort
import com.apptolast.invernaderos.features.alert.domain.port.output.AlertStateChangedEventPublisherPort
import com.apptolast.invernaderos.features.shared.domain.Either
import com.apptolast.invernaderos.features.shared.domain.model.TenantId
import java.time.Instant

/**
 * Plain Kotlin use case — no Spring annotations. The transactional boundary lives
 * one layer outwards in [com.apptolast.invernaderos.features.alert.infrastructure.adapter.input.AlertRestInboundAdapter]
 * so that the alert update, the state-change write and the AFTER_COMMIT echo to MQTT
 * all participate in the same metadataTransactionManager transaction.
 */
class ResolveAlertUseCaseImpl(
    private val repository: AlertRepositoryPort,
    private val stateChangePort: AlertStateChangePersistencePort,
    private val eventPublisher: AlertStateChangedEventPublisherPort
) : ResolveAlertUseCase {

    override fun resolve(id: Long, tenantId: TenantId, resolvedByUserId: Long?): Either<AlertError, Alert> {
        val existing = repository.findByIdAndTenantId(id, tenantId)
            ?: return Either.Left(AlertError.NotFound(id, tenantId))

        if (existing.isResolved) {
            return Either.Left(AlertError.AlreadyResolved(id))
        }

        val now = Instant.now()
        val resolved = existing.copy(
            isResolved = true,
            resolvedAt = now,
            resolvedByUserId = resolvedByUserId,
            updatedAt = now
        )

        val persisted = repository.save(resolved)
        val resolveActor: AlertActor = if (resolvedByUserId != null) {
            AlertActor.User(userId = resolvedByUserId, username = null, displayName = null)
        } else {
            AlertActor.System
        }
        val change = AlertStateChange(
            id = null,
            alertId = persisted.id ?: throw IllegalStateException("Alert ID cannot be null after save"),
            fromResolved = false,
            toResolved = true,
            source = AlertSignalSource.API,
            rawValue = null,
            at = now,
            actor = resolveActor,
        )
        val persistedChange = stateChangePort.save(change)
        eventPublisher.publish(persisted, persistedChange)

        return Either.Right(persisted)
    }

    override fun reopen(id: Long, tenantId: TenantId, actorUserId: Long?): Either<AlertError, Alert> {
        val existing = repository.findByIdAndTenantId(id, tenantId)
            ?: return Either.Left(AlertError.NotFound(id, tenantId))

        if (!existing.isResolved) {
            return Either.Left(AlertError.NotResolved(id))
        }

        val now = Instant.now()
        val reopened = existing.copy(
            isResolved = false,
            resolvedAt = null,
            resolvedByUserId = null,
            updatedAt = now
        )

        val persisted = repository.save(reopened)
        val reopenActor: AlertActor = if (actorUserId != null) {
            AlertActor.User(userId = actorUserId, username = null, displayName = null)
        } else {
            AlertActor.System
        }
        val change = AlertStateChange(
            id = null,
            alertId = persisted.id ?: throw IllegalStateException("Alert ID cannot be null after save"),
            fromResolved = true,
            toResolved = false,
            source = AlertSignalSource.API,
            rawValue = null,
            at = now,
            actor = reopenActor,
        )
        val persistedChange = stateChangePort.save(change)
        eventPublisher.publish(persisted, persistedChange)

        return Either.Right(persisted)
    }
}
