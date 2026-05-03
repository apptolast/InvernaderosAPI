package com.apptolast.invernaderos.features.alert.domain.model

import java.time.Instant

data class RecurrenceBucket(
    val key: String,
    val label: String,
    val count: Long,
    val lastSeenAt: Instant,
)
