package com.apptolast.invernaderos.features.alert.dto.response

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Actor who triggered the alert state transition. Shape is flat for client convenience.")
data class AlertActorResponse(
    @Schema(description = "Actor kind: USER, DEVICE, or SYSTEM", example = "USER", allowableValues = ["USER", "DEVICE", "SYSTEM"])
    val kind: String,
    @Schema(description = "User ID — populated only when kind=USER", example = "42")
    val userId: Long? = null,
    @Schema(description = "Username — populated only when kind=USER and hydrated via JOIN", example = "john.doe")
    val username: String? = null,
    @Schema(description = "Display name — populated only when kind=USER and hydrated via JOIN", example = "John Doe")
    val displayName: String? = null,
    @Schema(description = "Free-form reference — populated only when kind=DEVICE", example = "gw-001")
    val ref: String? = null,
)
