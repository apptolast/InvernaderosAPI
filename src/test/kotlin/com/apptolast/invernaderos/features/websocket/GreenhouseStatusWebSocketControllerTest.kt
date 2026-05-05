package com.apptolast.invernaderos.features.websocket

import com.apptolast.invernaderos.features.user.User
import com.apptolast.invernaderos.features.user.UserService
import com.apptolast.invernaderos.features.websocket.GreenhouseStatusWebSocketController.Companion.SOURCE_INITIAL_REQUEST
import com.apptolast.invernaderos.features.websocket.GreenhouseStatusWebSocketController.Companion.SOURCE_INITIAL_REQUEST_ALL
import com.apptolast.invernaderos.features.websocket.GreenhouseStatusWebSocketController.Companion.STATUS_SCOPE_HEADER
import com.apptolast.invernaderos.features.websocket.broadcast.WsDeliveryLogger
import com.apptolast.invernaderos.features.websocket.dto.GreenhouseStatusResponse
import com.apptolast.invernaderos.features.websocket.dto.TenantResponse
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.messaging.Message
import org.springframework.messaging.simp.stomp.StompCommand
import org.springframework.messaging.simp.stomp.StompHeaderAccessor
import org.springframework.messaging.support.MessageBuilder
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import java.security.Principal
import java.time.Instant

/**
 * Pins down the symmetric tenant-scoping contract of the WS handlers:
 *
 *  - Default `/status/request` → tenant-scoped for every role.
 *    `ROLE_ADMIN` no longer receives the cross-tenant snapshot by
 *    default — that's the user-visible behaviour change in this
 *    refactor.
 *  - `/status/request` with native header `X-Status-Scope: all` →
 *    cross-tenant snapshot, but ONLY when the caller is `ROLE_ADMIN`.
 *    Non-admins sending the header are rejected with
 *    [AccessDeniedException].
 *  - `/status/request/all-tenants` → admin-only equivalent of the
 *    header opt-in, useful for tooling that prefers a dedicated
 *    destination.
 *  - All previous defensive cases still hold (null/non-Authentication
 *    principal → 403, inactive user → 403, unknown email → 403).
 *
 * These tests call the controller directly — they do NOT exercise
 * Spring's argument-resolver chain. See
 * [GreenhouseStatusWebSocketControllerSpringInvocationTest] for the
 * counterpart that does.
 */
class GreenhouseStatusWebSocketControllerTest {

    private lateinit var assembler: GreenhouseStatusAssembler
    private lateinit var userService: UserService
    private lateinit var deliveryLogger: WsDeliveryLogger
    private lateinit var controller: GreenhouseStatusWebSocketController

    @BeforeEach
    fun setUp() {
        assembler = mockk()
        userService = mockk()
        deliveryLogger = mockk(relaxed = true)
        controller = GreenhouseStatusWebSocketController(assembler, userService, deliveryLogger)
    }

    // -------------------- Default endpoint, tenant scope --------------------

    @Test
    fun `null principal on message is rejected with AccessDeniedException`() {
        assertThatThrownBy { controller.getFullStatus(message(principal = null)) }
            .isInstanceOf(AccessDeniedException::class.java)
    }

    @Test
    fun `non-Authentication principal is rejected`() {
        val plainPrincipal = Principal { "alice@example.com" }
        assertThatThrownBy { controller.getFullStatus(message(principal = plainPrincipal)) }
            .isInstanceOf(AccessDeniedException::class.java)
    }

    @Test
    fun `ROLE_USER receives only their tenant snapshot`() {
        val user = userRow(id = 7L, email = "alice@example.com", tenantId = 42L, isActive = true)
        every { userService.findByEmail("alice@example.com") } returns user
        val tenantSnapshot = singleTenantResponse(tenantId = 42L)
        every { assembler.assembleStatusForTenant(42L) } returns tenantSnapshot

        val response = controller.getFullStatus(
            message(principal = userAuth("alice@example.com", "ROLE_USER"), sessionId = "s1"),
        )

        assertThat(response.tenants).hasSize(1)
        assertThat(response.tenants[0].id).isEqualTo(42L)
        verify(exactly = 1) { assembler.assembleStatusForTenant(42L) }
        verify(exactly = 0) { assembler.assembleFullStatus() }
        verify { deliveryLogger.logDelivery("alice@example.com", listOf(42L), SOURCE_INITIAL_REQUEST, tenantSnapshot, "s1") }
    }

    @Test
    fun `ROLE_ADMIN without scope header is now tenant-scoped (regression vs PR 156)`() {
        val adminUser = userRow(id = 1L, email = "admin@example.com", tenantId = 99L, isActive = true)
        every { userService.findByEmail("admin@example.com") } returns adminUser
        val tenantSnapshot = singleTenantResponse(tenantId = 99L)
        every { assembler.assembleStatusForTenant(99L) } returns tenantSnapshot

        val response = controller.getFullStatus(
            message(principal = userAuth("admin@example.com", "ROLE_ADMIN")),
        )

        assertThat(response.tenants).hasSize(1)
        assertThat(response.tenants[0].id).isEqualTo(99L)
        verify(exactly = 1) { assembler.assembleStatusForTenant(99L) }
        verify(exactly = 0) { assembler.assembleFullStatus() }
    }

    @Test
    fun `ROLE_USER with inactive user is rejected`() {
        val user = userRow(id = 7L, email = "alice@example.com", tenantId = 42L, isActive = false)
        every { userService.findByEmail("alice@example.com") } returns user

        assertThatThrownBy {
            controller.getFullStatus(message(principal = userAuth("alice@example.com", "ROLE_USER")))
        }
            .isInstanceOf(AccessDeniedException::class.java)
            .hasMessageContaining("inactive")
        verify(exactly = 0) { assembler.assembleStatusForTenant(any()) }
    }

    @Test
    fun `ROLE_USER with unknown email is rejected`() {
        every { userService.findByEmail("ghost@example.com") } returns null

        assertThatThrownBy {
            controller.getFullStatus(message(principal = userAuth("ghost@example.com", "ROLE_USER")))
        }
            .isInstanceOf(AccessDeniedException::class.java)
            .hasMessageContaining("not found")
    }

    // -------------------- Default endpoint, scope=all opt-in ----------------

    @Test
    fun `ROLE_ADMIN with scope all returns full snapshot`() {
        val full = fullStatus(1L, 2L, 3L)
        every { assembler.assembleFullStatus() } returns full

        val response = controller.getFullStatus(
            message(
                principal = userAuth("admin@example.com", "ROLE_ADMIN"),
                scopeHeaderValue = "all",
            ),
        )

        assertThat(response.tenants).extracting<Long> { it.id }.containsExactly(1L, 2L, 3L)
        verify(exactly = 1) { assembler.assembleFullStatus() }
        verify(exactly = 0) { assembler.assembleStatusForTenant(any()) }
        verify(exactly = 0) { userService.findByEmail(any()) }
        verify { deliveryLogger.logDelivery("admin@example.com", listOf(1L, 2L, 3L), SOURCE_INITIAL_REQUEST_ALL, full, any()) }
    }

    @Test
    fun `scope all is case-insensitive`() {
        val full = fullStatus(1L, 2L)
        every { assembler.assembleFullStatus() } returns full

        val response = controller.getFullStatus(
            message(principal = userAuth("admin@example.com", "ROLE_ADMIN"), scopeHeaderValue = "ALL"),
        )

        assertThat(response.tenants).hasSize(2)
        verify(exactly = 1) { assembler.assembleFullStatus() }
    }

    @Test
    fun `ROLE_USER with scope all is rejected`() {
        assertThatThrownBy {
            controller.getFullStatus(
                message(
                    principal = userAuth("alice@example.com", "ROLE_USER"),
                    scopeHeaderValue = "all",
                ),
            )
        }
            .isInstanceOf(AccessDeniedException::class.java)
            .hasMessageContaining("ROLE_ADMIN")
        verify(exactly = 0) { assembler.assembleFullStatus() }
        verify(exactly = 0) { assembler.assembleStatusForTenant(any()) }
    }

    @Test
    fun `unrecognised scope value falls back to tenant scope`() {
        val user = userRow(id = 1L, email = "admin@example.com", tenantId = 99L, isActive = true)
        every { userService.findByEmail("admin@example.com") } returns user
        val tenantSnapshot = singleTenantResponse(99L)
        every { assembler.assembleStatusForTenant(99L) } returns tenantSnapshot

        controller.getFullStatus(
            message(principal = userAuth("admin@example.com", "ROLE_ADMIN"), scopeHeaderValue = "bogus"),
        )

        verify(exactly = 1) { assembler.assembleStatusForTenant(99L) }
        verify(exactly = 0) { assembler.assembleFullStatus() }
    }

    // -------------------- /all-tenants endpoint ------------------------------

    @Test
    fun `getAllTenantsStatus with ROLE_ADMIN returns full snapshot`() {
        val full = fullStatus(1L, 2L, 3L)
        every { assembler.assembleFullStatus() } returns full

        val response = controller.getAllTenantsStatus(
            message(principal = userAuth("admin@example.com", "ROLE_ADMIN"), sessionId = "s2"),
        )

        assertThat(response.tenants).extracting<Long> { it.id }.containsExactly(1L, 2L, 3L)
        verify(exactly = 1) { assembler.assembleFullStatus() }
        verify(exactly = 0) { userService.findByEmail(any()) }
        verify { deliveryLogger.logDelivery("admin@example.com", listOf(1L, 2L, 3L), SOURCE_INITIAL_REQUEST_ALL, full, "s2") }
    }

    @Test
    fun `getAllTenantsStatus with ROLE_USER is rejected`() {
        assertThatThrownBy {
            controller.getAllTenantsStatus(message(principal = userAuth("alice@example.com", "ROLE_USER")))
        }
            .isInstanceOf(AccessDeniedException::class.java)
            .hasMessageContaining("ROLE_ADMIN")
        verify(exactly = 0) { assembler.assembleFullStatus() }
    }

    @Test
    fun `getAllTenantsStatus without principal is rejected`() {
        assertThatThrownBy { controller.getAllTenantsStatus(message(principal = null)) }
            .isInstanceOf(AccessDeniedException::class.java)
    }

    // ------------------------------------------------------------------
    // helpers
    // ------------------------------------------------------------------

    private fun message(
        principal: Principal?,
        sessionId: String? = "test-session-1",
        scopeHeaderValue: String? = null,
    ): Message<ByteArray> {
        val accessor = StompHeaderAccessor.create(StompCommand.SEND)
        accessor.destination = "/app/status/request"
        accessor.sessionId = sessionId
        if (principal != null) accessor.user = principal
        if (scopeHeaderValue != null) accessor.setNativeHeader(STATUS_SCOPE_HEADER, scopeHeaderValue)
        return MessageBuilder.createMessage(ByteArray(0), accessor.messageHeaders)
    }

    private fun userAuth(email: String, vararg authorities: String) =
        UsernamePasswordAuthenticationToken(
            email,
            null,
            authorities.map<String, GrantedAuthority> { SimpleGrantedAuthority(it) },
        )

    private fun userRow(id: Long, email: String, tenantId: Long, isActive: Boolean) =
        User(
            id = id,
            code = "USR-${id.toString().padStart(5, '0')}",
            tenantId = tenantId,
            username = email.substringBefore("@"),
            email = email,
            passwordHash = "ignored",
            role = "USER",
            isActive = isActive,
        )

    private fun singleTenantResponse(tenantId: Long) = GreenhouseStatusResponse(
        timestamp = Instant.parse("2026-05-04T22:15:00Z"),
        tenants = listOf(tenantResponse(tenantId)),
    )

    private fun fullStatus(vararg tenantIds: Long) = GreenhouseStatusResponse(
        timestamp = Instant.parse("2026-05-04T22:15:00Z"),
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
