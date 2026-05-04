package com.apptolast.invernaderos.config

import com.apptolast.invernaderos.core.security.JwtService
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
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException

/**
 * Unit tests for the strict (no-anonymous-fallback) STOMP CONNECT auth.
 * Pins down the security contract: anything other than a CONNECT carrying a
 * valid Bearer JWT must be rejected before reaching downstream handlers.
 *
 * Uses MockK on the collaborators ([JwtService], [UserDetailsService]) and a
 * real Spring [Message] built via [MessageBuilder] so we exercise the same
 * accessor wiring the framework uses at runtime.
 */
class StompJwtAuthInterceptorTest {

    private lateinit var jwtService: JwtService
    private lateinit var userDetailsService: UserDetailsService
    private lateinit var interceptor: StompJwtAuthInterceptor

    @BeforeEach
    fun setUp() {
        jwtService = mockk()
        userDetailsService = mockk()
        interceptor = StompJwtAuthInterceptor(jwtService, userDetailsService)
    }

    // ------------------------------------------------------------------
    // CONNECT — rejection paths
    // ------------------------------------------------------------------

    @Test
    fun `CONNECT without Authorization header is rejected`() {
        val message = connectMessage(authHeader = null)

        assertThatThrownBy { interceptor.preSend(message, mockk(relaxed = true)) }
            .isInstanceOf(AccessDeniedException::class.java)
            .hasMessageContaining("Bearer")
    }

    @Test
    fun `CONNECT with non-Bearer Authorization header is rejected`() {
        val message = connectMessage(authHeader = "Basic dXNlcjpwYXNz")

        assertThatThrownBy { interceptor.preSend(message, mockk(relaxed = true)) }
            .isInstanceOf(AccessDeniedException::class.java)
    }

    @Test
    fun `CONNECT with empty Bearer token is rejected`() {
        val message = connectMessage(authHeader = "Bearer    ")

        assertThatThrownBy { interceptor.preSend(message, mockk(relaxed = true)) }
            .isInstanceOf(AccessDeniedException::class.java)
            .hasMessageContaining("empty")
    }

    @Test
    fun `CONNECT with malformed JWT is rejected`() {
        every { jwtService.extractUsername("garbage") } throws RuntimeException("malformed")
        val message = connectMessage(authHeader = "Bearer garbage")

        assertThatThrownBy { interceptor.preSend(message, mockk(relaxed = true)) }
            .isInstanceOf(AccessDeniedException::class.java)
            .hasMessageContaining("malformed")
    }

    @Test
    fun `CONNECT with valid JWT but unknown user is rejected`() {
        every { jwtService.extractUsername("tok") } returns "ghost@example.com"
        every { userDetailsService.loadUserByUsername("ghost@example.com") } throws
            UsernameNotFoundException("not found")

        val message = connectMessage(authHeader = "Bearer tok")

        assertThatThrownBy { interceptor.preSend(message, mockk(relaxed = true)) }
            .isInstanceOf(AccessDeniedException::class.java)
            .hasMessageContaining("not found")
    }

    @Test
    fun `CONNECT with expired or signature-mismatched JWT is rejected`() {
        val userDetails = userDetails("alice@example.com", "ROLE_USER")
        every { jwtService.extractUsername("tok") } returns "alice@example.com"
        every { userDetailsService.loadUserByUsername("alice@example.com") } returns userDetails
        every { jwtService.isTokenValid("tok", userDetails) } returns false

        val message = connectMessage(authHeader = "Bearer tok")

        assertThatThrownBy { interceptor.preSend(message, mockk(relaxed = true)) }
            .isInstanceOf(AccessDeniedException::class.java)
            .hasMessageContaining("invalid or expired")
    }

    // ------------------------------------------------------------------
    // CONNECT — happy path
    // ------------------------------------------------------------------

    @Test
    fun `CONNECT with valid JWT binds principal and returns the message`() {
        val userDetails = userDetails("alice@example.com", "ROLE_USER")
        every { jwtService.extractUsername("tok") } returns "alice@example.com"
        every { userDetailsService.loadUserByUsername("alice@example.com") } returns userDetails
        every { jwtService.isTokenValid("tok", userDetails) } returns true

        val message = connectMessage(authHeader = "Bearer tok")
        val result = interceptor.preSend(message, mockk(relaxed = true))

        val accessor = StompHeaderAccessor.wrap(result)
        assertThat(accessor.user).isNotNull
        assertThat(accessor.user!!.name).isEqualTo("alice@example.com")
        verify(exactly = 1) { jwtService.isTokenValid("tok", userDetails) }
    }

    // ------------------------------------------------------------------
    // SEND / SUBSCRIBE — defense in depth
    // ------------------------------------------------------------------

    @Test
    fun `SEND without bound principal is rejected`() {
        val message = nonConnectMessage(StompCommand.SEND, principal = null)

        assertThatThrownBy { interceptor.preSend(message, mockk(relaxed = true)) }
            .isInstanceOf(AccessDeniedException::class.java)
            .hasMessageContaining("SEND")
    }

    @Test
    fun `SUBSCRIBE without bound principal is rejected`() {
        val message = nonConnectMessage(StompCommand.SUBSCRIBE, principal = null)

        assertThatThrownBy { interceptor.preSend(message, mockk(relaxed = true)) }
            .isInstanceOf(AccessDeniedException::class.java)
            .hasMessageContaining("SUBSCRIBE")
    }

    @Test
    fun `SEND with bound principal passes through`() {
        val authToken = org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
            "alice@example.com", null, listOf<GrantedAuthority>(SimpleGrantedAuthority("ROLE_USER"))
        )
        val message = nonConnectMessage(StompCommand.SEND, principal = authToken)

        val result = interceptor.preSend(message, mockk(relaxed = true))

        // Same message returned, principal preserved.
        val accessor = StompHeaderAccessor.wrap(result)
        assertThat(accessor.user).isSameAs(authToken)
    }

    @Test
    fun `non-STOMP command messages pass through untouched`() {
        // No StompHeaderAccessor on the message → return early without throwing.
        val plain = MessageBuilder.withPayload("ignored").build()
        val result = interceptor.preSend(plain, mockk(relaxed = true))
        assertThat(result).isSameAs(plain)
    }

    // ------------------------------------------------------------------
    // helpers
    // ------------------------------------------------------------------

    private fun connectMessage(authHeader: String?): Message<*> {
        val accessor = StompHeaderAccessor.create(StompCommand.CONNECT)
        accessor.sessionId = "test-session-1"
        // setLeaveMutable(true) keeps the accessor attached to the message
        // headers so the interceptor's MessageHeaderAccessor.getAccessor(...)
        // returns the same mutable instance it would receive at runtime.
        // Without this, accessor.user = ... has no observable effect.
        accessor.setLeaveMutable(true)
        if (authHeader != null) accessor.setNativeHeader("Authorization", authHeader)
        return MessageBuilder.createMessage("".toByteArray(), accessor.messageHeaders)
    }

    private fun nonConnectMessage(
        command: StompCommand,
        principal: java.security.Principal?,
    ): Message<*> {
        val accessor = StompHeaderAccessor.create(command)
        accessor.sessionId = "test-session-2"
        accessor.setLeaveMutable(true)
        if (principal != null) accessor.user = principal
        return MessageBuilder.createMessage("".toByteArray(), accessor.messageHeaders)
    }

    private fun userDetails(email: String, role: String): UserDetails {
        return org.springframework.security.core.userdetails.User.builder()
            .username(email)
            .password("ignored")
            .authorities(SimpleGrantedAuthority(role))
            .build()
    }
}
