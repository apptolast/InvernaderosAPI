package com.apptolast.invernaderos.features.alert.domain.model

data class ByActorBucket(
    val actorUserId: Long,
    val username: String?,
    val displayName: String?,
    val count: Long,
)
