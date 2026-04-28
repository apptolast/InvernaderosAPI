package com.apptolast.invernaderos.features.alert.infrastructure.adapter.input

import com.apptolast.invernaderos.features.alert.domain.error.AlertError
import com.apptolast.invernaderos.features.alert.domain.model.Alert
import com.apptolast.invernaderos.features.alert.domain.port.input.ResolveAlertUseCase
import com.apptolast.invernaderos.features.shared.domain.Either
import com.apptolast.invernaderos.features.shared.domain.model.TenantId
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

/**
 * Input adapter: bridges the REST transport layer into the alert domain use case.
 *
 * The @Transactional boundary lives here (rather than on the use case or the controller)
 * so that the plain-Kotlin use case stays framework-free while the three writes —
 * alert update, state change persistence and AFTER_COMMIT echo to MQTT — share a single
 * metadata transaction.
 *
 * Symmetric to [AlertMqttInboundAdapter].
 */
@Component
class AlertRestInboundAdapter(
    private val resolveUseCase: ResolveAlertUseCase
) {

    @Transactional("metadataTransactionManager")
    fun resolve(id: Long, tenantId: TenantId, resolvedByUserId: Long?): Either<AlertError, Alert> =
        resolveUseCase.resolve(id, tenantId, resolvedByUserId)

    @Transactional("metadataTransactionManager")
    fun reopen(id: Long, tenantId: TenantId): Either<AlertError, Alert> =
        resolveUseCase.reopen(id, tenantId)
}
