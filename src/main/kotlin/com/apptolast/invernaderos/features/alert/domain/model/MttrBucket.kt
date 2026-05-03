package com.apptolast.invernaderos.features.alert.domain.model

data class MttrBucket(
    val key: String,
    val label: String,
    val mttrSeconds: Double,
    val p50Seconds: Double,
    val p95Seconds: Double,
    val p99Seconds: Double,
    val sampleSize: Long,
)
