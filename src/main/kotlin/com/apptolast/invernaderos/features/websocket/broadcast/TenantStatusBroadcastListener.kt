package com.apptolast.invernaderos.features.websocket.broadcast

import com.apptolast.invernaderos.features.alert.infrastructure.adapter.output.AlertStateChangedEvent
import com.apptolast.invernaderos.features.websocket.event.DeviceCurrentValuesFlushedEvent
import com.apptolast.invernaderos.features.websocket.event.TenantStatusChangedEvent
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

/**
 * Single fan-in listener for every event that should produce a per-tenant
 * WebSocket push. Today there are three sources:
 *
 *  1. [DeviceCurrentValuesFlushedEvent] — emitted by `DeviceStatusProcessor`
 *     after the Timescale flush commits a batch of `device_current_values`
 *     upserts. Carries the set of tenant ids whose state changed.
 *  2. [AlertStateChangedEvent] — already published by the alert use cases.
 *     We add this listener alongside the existing
 *     [com.apptolast.invernaderos.features.push.infrastructure.adapter.output.AlertActivationPushListener]
 *     (which handles FCM). Spring delivers the event to both — they have
 *     unrelated responsibilities.
 *  3. [TenantStatusChangedEvent] — emitted by CRUD use cases (greenhouses,
 *     sectors, devices, settings, users) when admin-side mutations change
 *     the tenant's catalog.
 *
 * **AFTER_COMMIT** for events tied to a database transaction: avoids
 * publishing a snapshot the database will roll back. The
 * [AlertStateChangedEvent] listener is also AFTER_COMMIT because the
 * publisher (`AlertStateChangedEventPublisherAdapter`) sits inside the
 * use case's `@Transactional`.
 *
 * **No coalescing** is implemented here. The natural rate from the sensor
 * flush is ≤1 event/s/tenant, CRUD/alert events are rare. The downstream
 * `wsBroadcastExecutor` queue with `DiscardOldestPolicy` shields against
 * unforeseen bursts — the *newest* snapshot always wins.
 */
@Component
class TenantStatusBroadcastListener(
    private val wsBroadcaster: WsBroadcaster
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    fun onDeviceCurrentValuesFlushed(event: DeviceCurrentValuesFlushedEvent) {
        if (event.tenantIds.isEmpty()) return
        logger.debug("DeviceCurrentValuesFlushedEvent received for {} tenants", event.tenantIds.size)
        event.tenantIds.forEach { tenantId ->
            try {
                wsBroadcaster.broadcastTenantStatus(tenantId)
            } catch (e: Exception) {
                // Async submit can fail under extreme pressure
                // (DiscardOldestPolicy never throws here, but defensive in
                // case the executor is shutting down).
                logger.warn("Failed to enqueue WS broadcast for tenantId={}: {}",
                    tenantId, e.message)
            }
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    fun onAlertStateChanged(event: AlertStateChangedEvent) {
        val tenantId = event.alert.tenantId.value
        logger.debug("AlertStateChangedEvent → broadcast tenantId={} alertCode={} toResolved={}",
            tenantId, event.alert.code, event.change.toResolved)
        try {
            wsBroadcaster.broadcastTenantStatus(tenantId)
        } catch (e: Exception) {
            logger.warn("Failed to enqueue WS broadcast for tenantId={}: {}", tenantId, e.message)
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    fun onTenantStatusChanged(event: TenantStatusChangedEvent) {
        logger.debug("TenantStatusChangedEvent received: tenantId={} source={}",
            event.tenantId, event.source)
        try {
            wsBroadcaster.broadcastTenantStatus(event.tenantId)
        } catch (e: Exception) {
            logger.warn("Failed to enqueue WS broadcast for tenantId={}: {}",
                event.tenantId, e.message)
        }
    }
}
