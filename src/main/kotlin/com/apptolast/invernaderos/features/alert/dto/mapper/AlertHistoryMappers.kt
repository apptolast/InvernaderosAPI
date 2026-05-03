package com.apptolast.invernaderos.features.alert.dto.mapper

import com.apptolast.invernaderos.features.alert.domain.model.ActiveDurationBucket
import com.apptolast.invernaderos.features.alert.domain.model.AlertActor
import com.apptolast.invernaderos.features.alert.domain.model.AlertEpisode
import com.apptolast.invernaderos.features.alert.domain.model.AlertStatsSummary
import com.apptolast.invernaderos.features.alert.domain.model.AlertTransition
import com.apptolast.invernaderos.features.alert.domain.model.ByActorBucket
import com.apptolast.invernaderos.features.alert.domain.model.MttrBucket
import com.apptolast.invernaderos.features.alert.domain.model.RecurrenceBucket
import com.apptolast.invernaderos.features.alert.domain.model.TimeseriesDataPoint
import com.apptolast.invernaderos.features.alert.dto.response.ActiveDurationBucketResponse
import com.apptolast.invernaderos.features.alert.dto.response.AlertActorResponse
import com.apptolast.invernaderos.features.alert.dto.response.AlertEpisodeResponse
import com.apptolast.invernaderos.features.alert.dto.response.AlertStatsSummaryResponse
import com.apptolast.invernaderos.features.alert.dto.response.AlertTransitionResponse
import com.apptolast.invernaderos.features.alert.dto.response.ByActorBucketResponse
import com.apptolast.invernaderos.features.alert.dto.response.MttrBucketResponse
import com.apptolast.invernaderos.features.alert.dto.response.PagedResponse
import com.apptolast.invernaderos.features.alert.dto.response.RecurrenceBucketResponse
import com.apptolast.invernaderos.features.alert.dto.response.TimeseriesDataPointResponse
import com.apptolast.invernaderos.features.shared.domain.model.PagedResult

// --- AlertActor → AlertActorResponse ---

fun AlertActor.toResponse(): AlertActorResponse = when (this) {
    is AlertActor.User -> AlertActorResponse(
        kind = "USER",
        userId = userId,
        username = username,
        displayName = displayName,
        ref = null,
    )
    is AlertActor.Device -> AlertActorResponse(
        kind = "DEVICE",
        userId = null,
        username = null,
        displayName = null,
        ref = deviceRef,
    )
    AlertActor.System -> AlertActorResponse(
        kind = "SYSTEM",
        userId = null,
        username = null,
        displayName = null,
        ref = null,
    )
}

// --- AlertTransition → AlertTransitionResponse ---

fun AlertTransition.toResponse() = AlertTransitionResponse(
    transitionId = transitionId,
    at = at,
    fromResolved = fromResolved,
    toResolved = toResolved,
    source = source.name,
    rawValue = rawValue,
    actor = actor.toResponse(),
    alertId = alertId,
    alertCode = alertCode,
    alertMessage = alertMessage,
    alertTypeId = alertTypeId,
    alertTypeName = alertTypeName,
    severityId = severityId,
    severityName = severityName,
    severityLevel = severityLevel,
    severityColor = severityColor,
    sectorId = sectorId,
    sectorCode = sectorCode,
    greenhouseId = greenhouseId,
    greenhouseName = greenhouseName,
    tenantId = tenantId,
    previousTransitionAt = previousTransitionAt,
    episodeStartedAt = episodeStartedAt,
    episodeDurationSeconds = episodeDurationSeconds,
    occurrenceNumber = occurrenceNumber,
    totalTransitionsSoFar = totalTransitionsSoFar,
)

// --- AlertEpisode → AlertEpisodeResponse ---

fun AlertEpisode.toResponse() = AlertEpisodeResponse(
    alertId = alertId,
    alertCode = alertCode,
    triggeredAt = triggeredAt,
    resolvedAt = resolvedAt,
    durationSeconds = durationSeconds,
    triggerSource = triggerSource.name,
    resolveSource = resolveSource?.name,
    triggerActor = triggerActor.toResponse(),
    resolveActor = resolveActor?.toResponse(),
    severityId = severityId,
    severityName = severityName,
    sectorId = sectorId,
    sectorCode = sectorCode,
)

// --- PagedResult → PagedResponse ---

fun <T, R> PagedResult<T>.toResponse(mapper: (T) -> R) = PagedResponse(
    items = items.map(mapper),
    page = page,
    size = size,
    total = total,
    hasMore = hasMore,
)

// --- Stats domain → response ---

fun RecurrenceBucket.toResponse() = RecurrenceBucketResponse(
    key = key,
    label = label,
    count = count,
    lastSeenAt = lastSeenAt,
)

fun MttrBucket.toResponse() = MttrBucketResponse(
    key = key,
    label = label,
    mttrSeconds = mttrSeconds,
    p50Seconds = p50Seconds,
    p95Seconds = p95Seconds,
    p99Seconds = p99Seconds,
    sampleSize = sampleSize,
)

fun TimeseriesDataPoint.toResponse() = TimeseriesDataPointResponse(
    bucketStart = bucketStart,
    key = key,
    opened = opened,
    closed = closed,
)

fun ActiveDurationBucket.toResponse() = ActiveDurationBucketResponse(
    key = key,
    label = label,
    totalActiveSeconds = totalActiveSeconds,
)

fun ByActorBucket.toResponse() = ByActorBucketResponse(
    actorUserId = actorUserId,
    username = username,
    displayName = displayName,
    count = count,
)

fun AlertStatsSummary.toResponse() = AlertStatsSummaryResponse(
    totalActiveNow = totalActiveNow,
    openedToday = openedToday,
    closedToday = closedToday,
    mttrTodaySeconds = mttrTodaySeconds,
    top3RecurrentCodesThisWeek = top3RecurrentCodesThisWeek,
)
