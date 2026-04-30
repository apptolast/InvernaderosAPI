package com.apptolast.invernaderos.features.push.infrastructure.adapter.input

import com.apptolast.invernaderos.features.push.PushTokenService
import com.apptolast.invernaderos.features.push.dto.request.PushTokenRegisterRequest
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*

/**
 * Endpoints REST para que los clientes registren / desregistren su token FCM
 * tras login y logout. Ambos requieren JWT — el `tenantId` y `userId` se
 * resuelven desde la identidad autenticada, NUNCA desde el body, para que un
 * cliente no pueda registrar tokens de otro tenant.
 */
@RestController
@RequestMapping("/api/v1/push-tokens")
@CrossOrigin(origins = ["*"])
@Tag(name = "Push Tokens", description = "Registro de tokens FCM de dispositivos para notificaciones push")
@SecurityRequirement(name = "bearerAuth")
class PushTokenController(
    private val pushTokenService: PushTokenService
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    @PostMapping
    @Operation(summary = "Registrar (o refrescar) el token FCM de este dispositivo")
    fun register(
        @Valid @RequestBody request: PushTokenRegisterRequest,
        authentication: Authentication
    ): ResponseEntity<Void> {
        logger.debug(
            "POST /api/v1/push-tokens user={} platform={}",
            authentication.name, request.platform
        )
        pushTokenService.register(
            authenticatedPrincipal = authentication.name,
            token = request.token,
            platform = request.platform
        )
        return ResponseEntity.noContent().build()
    }

    @DeleteMapping("/{token}")
    @Operation(summary = "Desregistrar el token FCM (logout o invalidación)")
    fun unregister(
        @PathVariable token: String,
        authentication: Authentication
    ): ResponseEntity<Void> {
        logger.debug("DELETE /api/v1/push-tokens user={}", authentication.name)
        pushTokenService.unregister(token)
        return ResponseEntity.noContent().build()
    }
}
