package com.apptolast.invernaderos.features.websocket

import com.apptolast.invernaderos.features.user.User
import com.apptolast.invernaderos.features.user.UserService
import com.apptolast.invernaderos.features.websocket.GreenhouseStatusWebSocketController.Companion.STATUS_SCOPE_HEADER
import com.apptolast.invernaderos.features.websocket.broadcast.WsDeliveryLogger
import com.apptolast.invernaderos.features.websocket.dto.GreenhouseStatusResponse
import com.apptolast.invernaderos.features.websocket.dto.TenantResponse
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.core.convert.support.DefaultConversionService
import org.springframework.messaging.Message
import org.springframework.messaging.converter.StringMessageConverter
import org.springframework.messaging.handler.annotation.support.HeaderMethodArgumentResolver
import org.springframework.messaging.handler.annotation.support.HeadersMethodArgumentResolver
import org.springframework.messaging.handler.annotation.support.MessageMethodArgumentResolver
import org.springframework.messaging.handler.invocation.HandlerMethodArgumentResolverComposite
import org.springframework.messaging.handler.invocation.InvocableHandlerMethod
import org.springframework.messaging.simp.annotation.support.PrincipalMethodArgumentResolver
import org.springframework.messaging.simp.stomp.StompCommand
import org.springframework.messaging.simp.stomp.StompHeaderAccessor
import org.springframework.messaging.support.MessageBuilder
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import java.time.Instant

/**
 * Regression guards for the Spring messaging argument-resolver chain.
 *
 * The previous controller-level unit test used MockK and called the
 * Kotlin function directly — it never went through Spring's
 * argument-resolver chain, so it could not detect a Kotlin-vs-Spring
 * incompatibility (e.g. `Optional<Principal>` wrapping when the
 * parameter is declared `Principal?`). This test wires the same
 * resolver lineup that
 * [org.springframework.messaging.simp.annotation.support.SimpAnnotationMethodMessageHandler]
 * uses at runtime, so any future controller change that re-introduces
 * a resolver-incompatible parameter (Kotlin `Principal?`,
 * `Optional<X>` mismatched with the JVM signature, etc.) fails this
 * test instead of crashing real clients.
 *
 * Both `@MessageMapping` methods are covered, plus the admin opt-in
 * with `X-Status-Scope: all` to make sure the native-header path
 * also survives Spring's resolution chain.
 */
class GreenhouseStatusWebSocketControllerSpringInvocationTest {

    @Test
    fun `getFullStatus is invokable via Spring resolver chain (default tenant scope)`() {
        val (controller, _) = newController { assembler, userService ->
            val user = User(
                id = 7L, code = "USR-00007", tenantId = 42L, username = "alice",
                email = "alice@example.com", passwordHash = "ignored", role = "USER", isActive = true,
            )
            every { userService.findByEmail("alice@example.com") } returns user
            every { assembler.assembleStatusForTenant(42L) } returns singleTenantResponse(42L)
        }
        val message = sendMessage(
            destination = "/app/status/request",
            principal = principal("alice@example.com", "ROLE_USER"),
        )

        val result = invoke(controller, "getFullStatus", message) as GreenhouseStatusResponse

        assertThat(result.tenants).hasSize(1)
        assertThat(result.tenants[0].id).isEqualTo(42L)
    }

    @Test
    fun `getFullStatus admin with X-Status-Scope all returns full snapshot`() {
        val (controller, _) = newController { assembler, _ ->
            every { assembler.assembleFullStatus() } returns fullStatus(1L, 2L, 3L)
        }
        val message = sendMessage(
            destination = "/app/status/request",
            principal = principal("admin@example.com", "ROLE_ADMIN"),
            nativeHeaders = mapOf(STATUS_SCOPE_HEADER to "all"),
        )

        val result = invoke(controller, "getFullStatus", message) as GreenhouseStatusResponse

        assertThat(result.tenants).extracting<Long> { it.id }.containsExactly(1L, 2L, 3L)
    }

    @Test
    fun `getAllTenantsStatus admin returns full snapshot via Spring resolver chain`() {
        val (controller, _) = newController { assembler, _ ->
            every { assembler.assembleFullStatus() } returns fullStatus(10L, 20L)
        }
        val message = sendMessage(
            destination = "/app/status/request/all-tenants",
            principal = principal("admin@example.com", "ROLE_ADMIN"),
        )

        val result = invoke(controller, "getAllTenantsStatus", message) as GreenhouseStatusResponse

        assertThat(result.tenants).extracting<Long> { it.id }.containsExactly(10L, 20L)
    }

    // ------------------------------------------------------------------
    // helpers
    // ------------------------------------------------------------------

    private fun newController(
        stub: (assembler: GreenhouseStatusAssembler, userService: UserService) -> Unit,
    ): Pair<GreenhouseStatusWebSocketController, WsDeliveryLogger> {
        val assembler = mockk<GreenhouseStatusAssembler>()
        val userService = mockk<UserService>()
        val deliveryLogger = mockk<WsDeliveryLogger>(relaxed = true)
        stub(assembler, userService)
        return GreenhouseStatusWebSocketController(assembler, userService, deliveryLogger) to deliveryLogger
    }

    private fun invoke(
        controller: GreenhouseStatusWebSocketController,
        methodName: String,
        message: Message<*>,
    ): Any? {
        val resolvers = HandlerMethodArgumentResolverComposite().apply {
            addResolver(HeaderMethodArgumentResolver(DefaultConversionService(), null))
            addResolver(HeadersMethodArgumentResolver())
            addResolver(PrincipalMethodArgumentResolver())
            addResolver(MessageMethodArgumentResolver(StringMessageConverter()))
        }
        val method = GreenhouseStatusWebSocketController::class.java
            .getMethod(methodName, Message::class.java)
        val handler = InvocableHandlerMethod(controller, method).apply {
            setMessageMethodArgumentResolvers(resolvers)
        }
        return handler.invoke(message)
    }

    private fun sendMessage(
        destination: String,
        principal: java.security.Principal,
        nativeHeaders: Map<String, String> = emptyMap(),
    ): Message<ByteArray> {
        val accessor = StompHeaderAccessor.create(StompCommand.SEND)
        accessor.destination = destination
        accessor.sessionId = "test-session-via-spring"
        accessor.user = principal
        nativeHeaders.forEach { (k, v) -> accessor.setNativeHeader(k, v) }
        return MessageBuilder.createMessage(ByteArray(0), accessor.messageHeaders)
    }

    private fun principal(email: String, vararg authorities: String) =
        UsernamePasswordAuthenticationToken(
            email,
            null,
            authorities.map<String, GrantedAuthority> { SimpleGrantedAuthority(it) },
        )

    private fun singleTenantResponse(tenantId: Long) = GreenhouseStatusResponse(
        timestamp = Instant.parse("2026-05-05T00:00:00Z"),
        tenants = listOf(tenantResponse(tenantId)),
    )

    private fun fullStatus(vararg tenantIds: Long) = GreenhouseStatusResponse(
        timestamp = Instant.parse("2026-05-05T00:00:00Z"),
        tenants = tenantIds.map { tenantResponse(it) },
    )

    private fun tenantResponse(id: Long) = TenantResponse(
        id = id,
        code = "TEN-${id.toString().padStart(5, '0')}",
        name = "Tenant-$id",
        email = "tenant$id@example.com",
        province = null,
        country = null,
        phone = null,
        location = null,
        isActive = true,
        users = emptyList(),
        greenhouses = emptyList(),
    )
}
