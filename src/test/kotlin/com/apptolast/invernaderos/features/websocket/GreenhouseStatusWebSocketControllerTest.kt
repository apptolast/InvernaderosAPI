package com.apptolast.invernaderos.features.websocket

import com.apptolast.invernaderos.features.user.User
import com.apptolast.invernaderos.features.user.UserService
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
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import java.time.Instant

/**
 * Pins down the tenant-scoping contract of the WS request-response handler:
 *
 *  - principal=null → AccessDeniedException (defense-in-depth even though
 *    [com.apptolast.invernaderos.config.StompJwtAuthInterceptor] guarantees
 *    one upstream).
 *  - ROLE_USER → exactly one tenant in the response (the user's own).
 *  - ROLE_ADMIN → all active tenants (consistent with TenantOwnershipAspect
 *    bypass commit a15b528 on REST).
 *  - inactive user / unknown email → AccessDeniedException.
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

    @Test
    fun `null principal is rejected with AccessDeniedException`() {
        assertThatThrownBy { controller.getFullStatus(principal = null, sessionId = "s1") }
            .isInstanceOf(AccessDeniedException::class.java)
    }

    @Test
    fun `non-Authentication principal is rejected`() {
        val plainPrincipal = java.security.Principal { "alice@example.com" }
        assertThatThrownBy { controller.getFullStatus(principal = plainPrincipal, sessionId = "s1") }
            .isInstanceOf(AccessDeniedException::class.java)
    }

    @Test
    fun `ROLE_USER receives only their tenant snapshot`() {
        val user = userRow(id = 7L, email = "alice@example.com", tenantId = 42L, isActive = true)
        every { userService.findByEmail("alice@example.com") } returns user

        val tenantSnapshot = singleTenantResponse(tenantId = 42L)
        every { assembler.assembleStatusForTenant(42L) } returns tenantSnapshot

        val response = controller.getFullStatus(
            principal = userAuth("alice@example.com", "ROLE_USER"),
            sessionId = "s1",
        )

        assertThat(response.tenants).hasSize(1)
        assertThat(response.tenants[0].id).isEqualTo(42L)
        verify(exactly = 1) { assembler.assembleStatusForTenant(42L) }
        verify(exactly = 0) { assembler.assembleFullStatus() }
        verify { deliveryLogger.logDelivery("alice@example.com", listOf(42L), "INITIAL_REQUEST", tenantSnapshot, "s1") }
    }

    @Test
    fun `ROLE_USER with inactive user is rejected`() {
        val user = userRow(id = 7L, email = "alice@example.com", tenantId = 42L, isActive = false)
        every { userService.findByEmail("alice@example.com") } returns user

        assertThatThrownBy {
            controller.getFullStatus(
                principal = userAuth("alice@example.com", "ROLE_USER"),
                sessionId = "s1",
            )
        }
            .isInstanceOf(AccessDeniedException::class.java)
            .hasMessageContaining("inactive")
        verify(exactly = 0) { assembler.assembleStatusForTenant(any()) }
    }

    @Test
    fun `ROLE_USER with unknown email is rejected`() {
        every { userService.findByEmail("ghost@example.com") } returns null

        assertThatThrownBy {
            controller.getFullStatus(
                principal = userAuth("ghost@example.com", "ROLE_USER"),
                sessionId = "s1",
            )
        }
            .isInstanceOf(AccessDeniedException::class.java)
            .hasMessageContaining("not found")
    }

    @Test
    fun `ROLE_ADMIN receives full snapshot regardless of tenant`() {
        val full = GreenhouseStatusResponse(
            timestamp = Instant.parse("2026-05-04T22:15:00Z"),
            tenants = listOf(
                tenantResponse(1L),
                tenantResponse(2L),
                tenantResponse(3L),
            ),
        )
        every { assembler.assembleFullStatus() } returns full

        val response = controller.getFullStatus(
            principal = userAuth("admin@example.com", "ROLE_ADMIN"),
            sessionId = "s1",
        )

        assertThat(response.tenants).extracting<Long> { it.id }.containsExactly(1L, 2L, 3L)
        verify(exactly = 1) { assembler.assembleFullStatus() }
        verify(exactly = 0) { assembler.assembleStatusForTenant(any()) }
        // Admin path must not require a UserRepository lookup.
        verify(exactly = 0) { userService.findByEmail(any()) }
    }

    // ------------------------------------------------------------------
    // helpers
    // ------------------------------------------------------------------

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
