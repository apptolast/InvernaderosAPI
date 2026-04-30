package com.apptolast.invernaderos.features.push.dto.request

import com.apptolast.invernaderos.features.push.PushPlatform
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size

@Schema(description = "Request para registrar un token FCM de un dispositivo cliente")
data class PushTokenRegisterRequest(

    @field:NotBlank(message = "El token es obligatorio")
    @field:Size(min = 10, max = 4096, message = "El token debe tener entre 10 y 4096 caracteres")
    @Schema(
        description = "Token FCM emitido por Firebase para este dispositivo",
        example = "fABcD1234..."
    )
    val token: String,

    @field:NotNull(message = "La plataforma es obligatoria")
    @Schema(description = "Plataforma del dispositivo", example = "ANDROID")
    val platform: PushPlatform
)
