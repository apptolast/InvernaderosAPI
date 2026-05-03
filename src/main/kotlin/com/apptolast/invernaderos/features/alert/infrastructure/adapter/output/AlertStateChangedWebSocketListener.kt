package com.apptolast.invernaderos.features.alert.infrastructure.adapter.output

import com.apptolast.invernaderos.features.alert.domain.model.AlertTransition
import com.apptolast.invernaderos.features.alert.dto.mapper.toResponse
import org.slf4j.LoggerFactory
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

/**
 * Listens for [AlertStateChangedEvent] and broadcasts a lightweight [AlertTransition]-derived
 * payload to the WebSocket topic `/topic/tenant/{tenantId}/alerts` after the transaction commits.
 *
 * Design rationale for the single-row construction approach:
 * Re-querying the database for the full window-function projection on every state change would
 * pay the cost of the window query (LAG, LAST_VALUE, ROW_NUMBER over all transitions for that
 * alert) for what is effectively a single-row push. Instead, we build an [AlertTransition]
 * directly from the in-memory event payload. The window fields (previousTransitionAt,
 * episodeStartedAt, episodeDurationSeconds, occurrenceNumber, totalTransitionsSoFar) are
 * left as best-effort defaults — the UI can request the full history endpoint if it needs
 * precise window values. This keeps the WS broadcast O(1) database cost.
 */
@Component
class AlertStateChangedWebSocketListener(
    private val messagingTemplate: SimpMessagingTemplate,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun onAlertStateChanged(event: AlertStateChangedEvent) {
        val alert = event.alert
        val change = event.change

        val transition = AlertTransition(
            transitionId = change.id ?: 0L,
            at = change.at,
            fromResolved = change.fromResolved,
            toResolved = change.toResolved,
            source = change.source,
            rawValue = change.rawValue,
            actor = change.actor,
            alertId = alert.id ?: 0L,
            alertCode = alert.code,
            alertMessage = alert.message,
            alertTypeId = alert.alertTypeId,
            alertTypeName = alert.alertTypeName,
            severityId = alert.severityId,
            severityName = alert.severityName,
            severityLevel = alert.severityLevel,
            severityColor = null,           // not available on domain Alert; enriched on read path
            sectorId = alert.sectorId.value,
            sectorCode = alert.sectorCode,
            greenhouseId = null,            // not available on domain Alert; enriched on read path
            greenhouseName = null,
            tenantId = alert.tenantId.value,
            previousTransitionAt = null,    // window value — not recomputed here; see class javadoc
            episodeStartedAt = null,
            episodeDurationSeconds = null,
            occurrenceNumber = 0L,
            totalTransitionsSoFar = 0L,
        )

        val topic = "/topic/tenant/${alert.tenantId.value}/alerts"
        logger.debug(
            "Broadcasting AlertStateChangedEvent to {} — alertId={}, toResolved={}",
            topic, alert.id, change.toResolved
        )
        messagingTemplate.convertAndSend(topic, transition.toResponse())
    }
}
