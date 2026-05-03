package com.apptolast.invernaderos.features.alert.domain.model

data class AlertStatsSummary(
    val totalActiveNow: Long,
    val openedToday: Long,
    val closedToday: Long,
    val mttrTodaySeconds: Double?,
    val top3RecurrentCodesThisWeek: List<String>,
)
