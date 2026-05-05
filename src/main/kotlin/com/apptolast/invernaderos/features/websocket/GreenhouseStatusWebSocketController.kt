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
 * type mismatch` — exactly the crash that hit dev on the first rollout.
 * `GreenhouseStatusWebSocketControllerSpringInvocationTest` exercises
 * Spring's real handler machinery so a regression here fails tests
 * instead of crashing real clients.
 *
 * **Tenant scoping (symmetric with broadcasts).** Both endpoints below
 * route data based on the authenticated principal — they never trust a
 * client-supplied `tenantId`:
 *
 *  - [getFullStatus] (`/app/status/request`): tenant-scoped by default
 *    for every role, including `ROLE_ADMIN`. The handler resolves the
 *    user's tenant from `User.tenantId` (the row in `metadata.users`,
 *    NOT NULL by schema). An admin that wants the cross-tenant view
 *    opts in by sending the STOMP native header `X-Status-Scope: all`,
 *    which routes to [assembleFullStatus]. A non-admin sending
 *    `scope=all` is rejected with [AccessDeniedException]. Default-safe.
 *  - [getAllTenantsStatus] (`/app/status/request/all-tenants`):
 *    explicitly admin-only. Same effect as `scope=all` on the default
 *    endpoint. Convenient for tools that prefer a separate destination
 *    over a header negotiation.
 *
 * The session is guaranteed authenticated by
 * [com.apptolast.invernaderos.config.StompJwtAuthInterceptor] (CONNECT
 * without a valid JWT is rejected before this handler is reachable),
 * but we re-validate defensively here because (a) tests can bypass the
 * interceptor and (b) any future Spring change that lets a frame past
 * CONNECT without a principal must not silently expose data.
 *
 * Result: clients no longer see the asymmetric "first message has all
 * tenants, subsequent broadcasts have one" pattern that motivated this
 * refactor — the initial request and the broadcast pipeline are now
 * symmetric for default callers.
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
        val auth = requireAuthenticated(accessor, "/status/request")
        val email = auth.name
        val isAdmin = auth.authorities.any { it.authority == ROLE_ADMIN }
        val scope = accessor.getFirstNativeHeader(STATUS_SCOPE_HEADER)?.lowercase()

        return if (scope == STATUS_SCOPE_ALL) {
            if (!isAdmin) {
                throw AccessDeniedException("$STATUS_SCOPE_HEADER=$STATUS_SCOPE_ALL requires ROLE_ADMIN")
            }
            respondAllTenants(email, accessor.sessionId)
        } else {
            respondTenantScoped(email, isAdmin, accessor.sessionId)
        }
    }

    @MessageMapping("/status/request/all-tenants")
    @SendToUser("/queue/status/response")
    fun getAllTenantsStatus(message: Message<*>): GreenhouseStatusResponse {
        val accessor = SimpMessageHeaderAccessor.wrap(message)
        val auth = requireAuthenticated(accessor, "/status/request/all-tenants")
        if (auth.authorities.none { it.authority == ROLE_ADMIN }) {
            throw AccessDeniedException("/status/request/all-tenants requires ROLE_ADMIN")
        }
        return respondAllTenants(auth.name, accessor.sessionId)
    }

    private fun requireAuthenticated(accessor: SimpMessageHeaderAccessor, destination: String): Authentication {
        return accessor.user as? Authentication
            ?: throw AccessDeniedException("WebSocket $destination requires authenticated session")
    }

    private fun respondTenantScoped(email: String, isAdmin: Boolean, sessionId: String?): GreenhouseStatusResponse {
        val user = userService.findByEmail(email)
            ?: throw AccessDeniedException("Authenticated principal not found in users table: $email")
        if (!user.isActive) {
            throw AccessDeniedException("Authenticated principal is inactive: $email")
        }
        val role = if (isAdmin) "ADMIN" else "USER"
        logger.info(
            "WS /status/request principal={} role={} scope=tenant tenantId={} sessionId={}",
            email, role, user.tenantId, sessionId,
        )
        val response = assembler.assembleStatusForTenant(user.tenantId)
        deliveryLogger.logDelivery(
            principal = email,
            tenantIds = response.tenants.map { it.id },
            source = SOURCE_INITIAL_REQUEST,
            snapshot = response,
            sessionId = sessionId,
        )
        return response
    }

    private fun respondAllTenants(email: String, sessionId: String?): GreenhouseStatusResponse {
        logger.info(
            "WS /status/request principal={} role=ADMIN scope=all sessionId={}",
            email, sessionId,
        )
        val response = assembler.assembleFullStatus()
        deliveryLogger.logDelivery(
            principal = email,
            tenantIds = response.tenants.map { it.id },
            source = SOURCE_INITIAL_REQUEST_ALL,
            snapshot = response,
            sessionId = sessionId,
        )
        return response
    }

    companion object {
        /** STOMP native header name for opt-in cross-tenant scope. Value compared case-insensitively. */
        const val STATUS_SCOPE_HEADER = "X-Status-Scope"
        const val STATUS_SCOPE_ALL = "all"
        const val ROLE_ADMIN = "ROLE_ADMIN"

        /** Source tag for the per-recipient `WsDeliveryLogger` line on the default tenant-scoped initial request. */
        const val SOURCE_INITIAL_REQUEST = "INITIAL_REQUEST"

        /** Source tag for the cross-tenant initial request (admin opt-in or `/all-tenants` destination). */
        const val SOURCE_INITIAL_REQUEST_ALL = "INITIAL_REQUEST_ALL"
    }
}
