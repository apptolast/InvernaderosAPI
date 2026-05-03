package com.apptolast.invernaderos.features.alert.domain.model

import java.time.Instant

data class AlertTransition(
    val transitionId: Long,
    val at: Instant,
    val fromResolved: Boolean,
    val toResolved: Boolean,
    val source: AlertSignalSource,
    val rawValue: String?,
    val actor: AlertActor,
    val alertId: Long,
    val alertCode: String,
    val alertMessage: String?,
    val alertTypeId: Short?,
    val alertTypeName: String?,
    val severityId: Short?,
    val severityName: String?,
    val severityLevel: Short?,
    val severityColor: String?,
    val sectorId: Long,
    val sectorCode: String?,
    val greenhouseId: Long?,
    val greenhouseName: String?,
    val tenantId: Long,
    val previousTransitionAt: Instant?,
    val episodeStartedAt: Instant?,
    val episodeDurationSeconds: Long?,
    val occurrenceNumber: Long,
    val totalTransitionsSoFar: Long,
)
