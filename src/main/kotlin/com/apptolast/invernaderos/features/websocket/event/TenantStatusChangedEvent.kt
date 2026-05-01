package com.apptolast.invernaderos.features.websocket.event

/**
 * ApplicationEvent published from CRUD use cases (greenhouses, sectors,
 * devices, settings, users) and from any other writer that affects the
 * shape or content of the snapshot served by
 * `GreenhouseStatusAssembler.assembleStatusForTenant`.
 *
 * The `source` enum is for observability only — listeners do not branch on
 * it. Logs and metrics correlate broadcasts with the originating use case.
 *
 * Published from inside the use case's `@Transactional`; the consumer is
 * a `@TransactionalEventListener(AFTER_COMMIT)` to avoid emitting events
 * that the database will roll back.
 */
data class TenantStatusChangedEvent(
    val tenantId: Long,
    val source: Source
) {
    enum class Source {
        ALERT,
        GREENHOUSE_CRUD,
        SECTOR_CRUD,
        DEVICE_CRUD,
        SETTING_CRUD,
        USER_CRUD
    }
}
