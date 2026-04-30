package com.apptolast.invernaderos.features.push.infrastructure.adapter.output

import com.apptolast.invernaderos.features.alert.infrastructure.adapter.output.AlertStateChangedEvent
import com.apptolast.invernaderos.features.catalog.AlertSeverityRepository
import com.apptolast.invernaderos.features.push.AlertPushPayload
import com.apptolast.invernaderos.features.push.FcmPushService
import com.apptolast.invernaderos.features.sector.SectorRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

/**
 * Escucha [AlertStateChangedEvent] y dispara una notificación push cuando una
 * alerta es ACTIVADA (transición de `isResolved=true` → `isResolved=false`).
 *
 * Filtros de envío:
 *  - Sólo activaciones (`change.toResolved == false`). Las resoluciones se
 *    propagan vía WebSocket pero NO disparan push (decisión de producto:
 *    evitar saturar al usuario con notificaciones positivas).
 *  - Sólo si la severidad de la alerta tiene `notify_push = true` en
 *    `metadata.alert_severities`. Permite a un admin silenciar severidades
 *    concretas con un simple UPDATE SQL.
 *
 * Phase = AFTER_COMMIT: el push se emite sólo después de que la transacción
 * que cambió el estado haga commit. Si la transacción hace rollback, no se
 * envía nada (evita "fantasmas": notificación de alerta sin alerta en BBDD).
 *
 * Aislamiento de errores: cualquier excepción en el envío FCM se loggea y se
 * absorbe — el envío de push NUNCA debe romper el flujo principal de alertas.
 *
 * Transacción separada (`Propagation.REQUIRES_NEW`) para los lookups de
 * `AlertSeverity` y `Sector`: como la transacción original ya se ha
 * comiteado, el listener abre una propia, breve, sólo para leer.
 */
@Component
class AlertActivationPushListener(
    private val fcmPushService: FcmPushService,
    private val alertSeverityRepository: AlertSeverityRepository,
    private val sectorRepository: SectorRepository
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(
        value = "metadataTransactionManager",
        propagation = Propagation.REQUIRES_NEW,
        readOnly = true
    )
    fun onAlertActivated(event: AlertStateChangedEvent) {
        val change = event.change
        val alert = event.alert

        if (change.toResolved) {
            logger.debug(
                "Alert {} state change is a RESOLUTION (toResolved=true) — push skipped",
                alert.code
            )
            return
        }

        try {
            val severityId = alert.severityId ?: run {
                logger.debug("Alert {} has null severityId — push skipped", alert.code)
                return
            }
            val severity = alertSeverityRepository.findById(severityId).orElse(null) ?: run {
                logger.warn(
                    "Alert {} references unknown severityId={} — push skipped",
                    alert.code, severityId
                )
                return
            }
            if (!severity.notifyPush) {
                logger.debug(
                    "Severity {} has notify_push=false — push skipped for alert {}",
                    severity.name, alert.code
                )
                return
            }

            val sectorIdValue = alert.sectorId.value
            val sector = sectorRepository.findById(sectorIdValue).orElse(null) ?: run {
                logger.warn(
                    "Alert {} references unknown sectorId={} — push skipped",
                    alert.code, sectorIdValue
                )
                return
            }

            val payload = AlertPushPayload(
                alertId = alert.id ?: error("Alert id null after AFTER_COMMIT — should be impossible"),
                alertCode = alert.code,
                tenantId = alert.tenantId.value,
                greenhouseId = sector.greenhouseId,
                sectorId = sectorIdValue,
                severityName = severity.name,
                severityLevel = severity.level,
                severityColor = severity.color,
                title = "Nueva alerta: ${severity.name}",
                body = alert.message
                    ?: alert.description
                    ?: alert.clientName
                    ?: alert.code,
                createdAt = alert.createdAt
            )

            fcmPushService.sendAlertToTenant(payload)
        } catch (ex: Exception) {
            logger.error(
                "Failed to send FCM push for alert {} (DB state already committed)",
                alert.code, ex
            )
        }
    }
}
