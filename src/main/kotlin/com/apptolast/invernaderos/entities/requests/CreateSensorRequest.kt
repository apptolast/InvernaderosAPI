package com.apptolast.invernaderos.entities.requests

import jakarta.validation.constraints.*
import java.util.*


data class CreateSensorRequest(
    @param:NotNull
    val greenhouseId: UUID,

    @param:NotBlank
    @param:Size(max = 50)
    val deviceId: String,

    @param:NotBlank
    @field:Pattern(regexp = "^(TEMPERATURE|HUMIDITY|VPD|PAR|CO2|VWC|EC|PH)_(INDOOR|OUTDOOR|SUBSTRATE|IRRIGATION)$")
    val sensorType: String,

    val minThreshold: @DecimalMin("0.0") Double? = null,

    val maxThreshold: @DecimalMin("0.0") Double? = null
)
