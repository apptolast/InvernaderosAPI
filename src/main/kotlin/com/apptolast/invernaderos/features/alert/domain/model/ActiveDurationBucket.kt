package com.apptolast.invernaderos.features.alert.domain.model

data class ActiveDurationBucket(
    val key: String,
    val label: String,
    val totalActiveSeconds: Long,
)
