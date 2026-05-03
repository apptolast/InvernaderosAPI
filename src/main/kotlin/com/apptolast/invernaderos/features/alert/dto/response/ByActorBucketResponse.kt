package com.apptolast.invernaderos.features.alert.dto.response

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Activity count per user actor (resolver or opener)")
data class ByActorBucketResponse(
    @Schema(description = "User ID of the actor") val actorUserId: Long,
    @Schema(description = "Username of the actor") val username: String?,
    @Schema(description = "Display name of the actor") val displayName: String?,
    @Schema(description = "Number of transitions attributed to this actor in the time range") val count: Long,
)
