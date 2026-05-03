package com.apptolast.invernaderos.features.alert.dto.request

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Request body for reopening a resolved alert")
data class AlertReopenRequest(
    @Schema(description = "ID of the user who is reopening the alert. Null = system actor.", example = "42")
    val actorUserId: Long? = null,
)
