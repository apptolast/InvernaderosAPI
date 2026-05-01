package com.apptolast.invernaderos.features.websocket.event

/**
 * ApplicationEvent published from `DeviceStatusProcessor.flushPendingChanges`
 * after the Timescale transaction commits a batch of `device_current_values`
 * upserts. Carries the set of tenant ids whose state may have changed in the
 * last second.
 *
 * Consumed by `TenantStatusBroadcastListener` to fan out a WebSocket push to
 * each affected tenant. The listener is `@TransactionalEventListener
 * (AFTER_COMMIT)` so a Timescale rollback does not leak ghost broadcasts.
 *
 * Set semantics — never empty when published; the publisher only fires when
 * the affected set is non-empty. A single event per flush minimises bus
 * pressure and is enough for the UI (the broadcast carries a fresh snapshot
 * either way).
 */
data class DeviceCurrentValuesFlushedEvent(
    val tenantIds: Set<Long>
)
