package com.apptolast.invernaderos.features.websocket

import com.apptolast.invernaderos.features.user.UserService
import com.apptolast.invernaderos.features.websocket.broadcast.WsDeliveryLogger
import com.apptolast.invernaderos.features.websocket.dto.GreenhouseStatusResponse
import org.slf4j.LoggerFactory
import org.springframework.messaging.Message
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.simp.SimpMessageHeaderAccessor
import org.springframework.messaging.simp.annotation.SendToUser
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.Authentication
import org.springframework.stereotype.Controller

/**
 * STOMP request-response controller for the greenhouse status snapshot.
 *
 * **Why a single `Message<*>` parameter** instead of binding `Principal` /
 * `@Header` directly: Spring's `PrincipalMethodArgumentResolver` (in
 * `spring-messaging` 6.2.x) wraps the resolved Principal in
 * `Optional.ofNullable(...)` whenever `MethodParameter.isOptional()` is
 * `true`, and Kotlin nullable types (`Principal?`) make `isOptional()`
 * return `true`. The JVM signature still expects a plain `Principal`, so
 * the reflective invocation explodes with `IllegalStateException: argument
 * type mismatch`. We hit exactly that crash on the dev rollout — see
 * `GreenhouseStatusWebSocketControllerSpringInvocationTest` which
 * exercises Spring's real handler machinery to keep the regression
 * pinned. Reading the principal and sessionId from the `Message`
 * sidesteps the resolver entirely.
 *
 * The session is guaranteed authenticated by
 * [com.apptolast.invernaderos.config.StompJwtAuthInterceptor] (CONNECT
 * without a valid JWT is rejected before this handler is reachable), but
 * we re-validate defensively here because (a) tests can bypass the
 * interceptor and (b) any future Spring change that lets a frame past
 * CONNECT without a principal must not silently expose data.
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
    fun getFullStatus(message: Message<*>): GreenhouseStatusResponse {
        val accessor = SimpMessageHeaderAccessor.wrap(message)
        val sessionId = accessor.sessionId
        val auth = accessor.user as? Authentication
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
