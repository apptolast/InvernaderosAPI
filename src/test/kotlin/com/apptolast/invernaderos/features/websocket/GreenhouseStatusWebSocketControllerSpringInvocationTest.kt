package com.apptolast.invernaderos.features.websocket

import com.apptolast.invernaderos.features.user.User
import com.apptolast.invernaderos.features.user.UserService
import com.apptolast.invernaderos.features.websocket.broadcast.WsDeliveryLogger
import com.apptolast.invernaderos.features.websocket.dto.GreenhouseStatusResponse
import com.apptolast.invernaderos.features.websocket.dto.TenantResponse
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.core.convert.support.DefaultConversionService
import org.springframework.messaging.handler.annotation.support.HeaderMethodArgumentResolver
import org.springframework.messaging.handler.annotation.support.HeadersMethodArgumentResolver
import org.springframework.messaging.handler.annotation.support.MessageMethodArgumentResolver
import org.springframework.messaging.converter.StringMessageConverter
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
 * Regression guard for the dev-rollout crash where Spring's
 * `PrincipalMethodArgumentResolver` wrapped a Kotlin nullable `Principal?`
 * parameter in `Optional<Principal>`, leaving the JVM signature
 * incompatible at reflective invocation time.
 *
 * The previous controller-level unit test used MockK and called the
 * Kotlin function directly — it never went through Spring's
 * argument-resolver chain, so it could not detect the wrapping. This
 * test wires the same resolver chain that
 * [org.springframework.messaging.simp.annotation.support.SimpAnnotationMethodMessageHandler]
 * uses at runtime, so any future controller change that re-introduces a
 * resolver-incompatible parameter (Kotlin `Principal?`, `Optional<X>`
 * mismatched with the JVM signature, etc.) fails this test instead of
 * crashing real clients in dev/prod.
 *
 * Specifically: this test will fail with `IllegalStateException: argument
 * type mismatch` exactly like the dev pod did, instead of silently
 * passing.
 */
class GreenhouseStatusWebSocketControllerSpringInvocationTest {

    @Test
    fun `controller is invokable by Spring messaging argument resolvers (regression for Optional Principal wrap)`() {
        val assembler = mockk<GreenhouseStatusAssembler>()
        val userService = mockk<UserService>()
        val deliveryLogger = mockk<WsDeliveryLogger>(relaxed = true)
        val controller = GreenhouseStatusWebSocketController(assembler, userService, deliveryLogger)

        // Same resolver lineup SimpAnnotationMethodMessageHandler uses at
        // runtime (excluding payload/destination resolvers — irrelevant
        // here because the controller takes `Message<*>`).
        val conversionService = DefaultConversionService()
        val messageConverter = StringMessageConverter()
        val resolvers = HandlerMethodArgumentResolverComposite().apply {
            addResolver(HeaderMethodArgumentResolver(conversionService, null))
            addResolver(HeadersMethodArgumentResolver())
            addResolver(PrincipalMethodArgumentResolver())
            addResolver(MessageMethodArgumentResolver(messageConverter))
        }

        val handler = InvocableHandlerMethod(
            controller,
            GreenhouseStatusWebSocketController::class.java
                .getMethod("getFullStatus", org.springframework.messaging.Message::class.java),
        ).apply { setMessageMethodArgumentResolvers(resolvers) }

        val tenantSnapshot = GreenhouseStatusResponse(
            timestamp = Instant.parse("2026-05-04T22:15:00Z"),
            tenants = listOf(tenantResponse(42L)),
        )
        val user = User(
            id = 7L,
            code = "USR-00007",
            tenantId = 42L,
            username = "alice",
            email = "alice@example.com",
            passwordHash = "ignored",
            role = "USER",
            isActive = true,
        )
        every { userService.findByEmail("alice@example.com") } returns user
        every { assembler.assembleStatusForTenant(42L) } returns tenantSnapshot

        val auth = UsernamePasswordAuthenticationToken(
            "alice@example.com",
            null,
            listOf<GrantedAuthority>(SimpleGrantedAuthority("ROLE_USER")),
        )
        val accessor = StompHeaderAccessor.create(StompCommand.SEND)
        accessor.destination = "/app/status/request"
        accessor.sessionId = "test-session-via-spring"
        accessor.user = auth
        val message = MessageBuilder.createMessage(ByteArray(0), accessor.messageHeaders)

        // This is the key invocation — it goes through Spring's resolver
        // chain just like the production handler dispatch. If the
        // controller signature regresses to a Kotlin nullable Principal
        // (or any other resolver-incompatible shape) this throws.
        val result = handler.invoke(message) as GreenhouseStatusResponse

        assertThat(result.tenants).hasSize(1)
        assertThat(result.tenants[0].id).isEqualTo(42L)
    }

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
