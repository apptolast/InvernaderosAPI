package com.apptolast.invernaderos.features.websocket

import com.apptolast.invernaderos.features.user.UserService
import com.apptolast.invernaderos.features.websocket.broadcast.WsDeliveryLogger
import com.apptolast.invernaderos.features.websocket.dto.GreenhouseStatusResponse
import org.slf4j.LoggerFactory
import org.springframework.messaging.handler.annotation.Header
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.simp.annotation.SendToUser
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.Authentication
import org.springframework.stereotype.Controller
import java.security.Principal

/**
 * STOMP request-response controller for the greenhouse status snapshot.
 *
 * The session is guaranteed authenticated by [com.apptolast.invernaderos.config.StompJwtAuthInterceptor]
 * (CONNECT without a valid JWT is rejected before this handler is reachable),
 * but we re-validate the principal here defensively because:
 *  - the controller may be invoked from tests that bypass the interceptor;
 *  - any future Spring change that lets a frame past CONNECT without a
 *    principal must not silently expose data.
 *
 * Tenant scoping:
 *  - `ROLE_ADMIN`: returns all active tenants ([assembleFullStatus]),
 *    consistent with the bypass in
 *    [com.apptolast.invernaderos.features.shared.security.TenantOwnershipAspect]
 *    (commit a15b528) for the REST surface.
 *  - everyone else: returns only the snapshot of the user's own tenant
 *    via [assembleStatusForTenant]. The tenant is resolved from the
 *    authenticated `User.tenantId`, never from a client-supplied parameter.
 */
@Controller
class GreenhouseStatusWebSocketController(
    private val assembler: GreenhouseStatusAssembler,
    private val userService: UserService,
    private val deliveryLogger: WsDeliveryLogger,
) {
    private val logger = LoggerFactory.getLogger(GreenhouseStatusWebSocketController::class.java)

    @MessageMapping("/status/request")
    @SendToUser("/queue/status/response")
    fun getFullStatus(
        principal: Principal?,
        @Header("simpSessionId", required = false) sessionId: String?,
    ): GreenhouseStatusResponse {
        val auth = principal as? Authentication
            ?: throw AccessDeniedException("WebSocket /status/request requires authenticated session")

        val email = auth.name
        val isAdmin = auth.authorities.any { it.authority == "ROLE_ADMIN" }

        val response = if (isAdmin) {
            logger.info("WS /status/request principal={} role=ADMIN scope=full sessionId={}", email, sessionId)
            assembler.assembleFullStatus()
        } else {
            val user = userService.findByEmail(email)
                ?: throw AccessDeniedException("Authenticated principal not found in users table: $email")
            if (!user.isActive) {
                throw AccessDeniedException("Authenticated principal is inactive: $email")
            }
            logger.info(
                "WS /status/request principal={} role=USER scope=tenant tenantId={} sessionId={}",
                email, user.tenantId, sessionId,
            )
            assembler.assembleStatusForTenant(user.tenantId)
        }

        deliveryLogger.logDelivery(
            principal = email,
            tenantIds = response.tenants.map { it.id },
            source = "INITIAL_REQUEST",
            snapshot = response,
            sessionId = sessionId,
        )
        return response
    }
}
