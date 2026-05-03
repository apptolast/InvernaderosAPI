package com.apptolast.invernaderos.features.alert.domain.model

import java.time.Instant

data class AlertEpisode(
    val alertId: Long,
    val alertCode: String,
    val triggeredAt: Instant,
    val resolvedAt: Instant?,
    val durationSeconds: Long?,
    val triggerSource: AlertSignalSource,
    val resolveSource: AlertSignalSource?,
    val triggerActor: AlertActor,
    val resolveActor: AlertActor?,
    val severityId: Short?,
    val severityName: String?,
    val sectorId: Long,
    val sectorCode: String?,
)
