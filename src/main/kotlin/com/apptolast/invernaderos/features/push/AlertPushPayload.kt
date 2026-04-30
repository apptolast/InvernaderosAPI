package com.apptolast.invernaderos.features.push

import java.time.Instant

/**
 * Payload de una notificación push generada a partir de la activación de
 * una alerta. Sólo datos primitivos: el `FcmPushService` los traduce a los
 * campos `notification` (display) y `data` (deep link) del `MulticastMessage`.
 *
 * Estructura `data` que recibe el cliente:
 *  - alertId, alertCode → identifican la alerta concreta
 *  - greenhouseId, sectorId → para el deep link a `GreenhouseDetailScreen`
 *  - severity, severityLevel → para colorear / filtrar en cliente
 *  - createdAt → ISO-8601 epoch millis
 */
data class AlertPushPayload(
    val alertId: Long,
    val alertCode: String,
    val tenantId: Long,
    val greenhouseId: Long,
    val sectorId: Long,
    val severityName: String,
    val severityLevel: Short,
    val severityColor: String?,
    val title: String,
    val body: String,
    val createdAt: Instant
)
