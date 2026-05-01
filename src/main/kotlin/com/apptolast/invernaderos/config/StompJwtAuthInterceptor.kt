package com.apptolast.invernaderos.config

import com.apptolast.invernaderos.core.security.JwtService
import org.slf4j.LoggerFactory
import org.springframework.messaging.Message
import org.springframework.messaging.MessageChannel
import org.springframework.messaging.simp.stomp.StompCommand
import org.springframework.messaging.simp.stomp.StompHeaderAccessor
import org.springframework.messaging.support.ChannelInterceptor
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.stereotype.Component

/**
 * STOMP CONNECT interceptor that extracts a Bearer JWT from the native
 * `Authorization` header, validates it with [JwtService], and attaches the
 * authenticated user as the STOMP session [java.security.Principal].
 *
 * Once attached, [org.springframework.messaging.simp.SimpMessagingTemplate.convertAndSendToUser]
 * can route messages to a specific username via `/user/{username}/queue/...`,
 * which is required for the future server-side broadcast path.
 *
 * **Backwards-compatible by design**: if the CONNECT frame omits the header,
 * brings a malformed/expired token, or the user lookup fails, the interceptor
 * leaves the session principal unset. Spring then falls back to the
 * sessionId-based anonymous principal, preserving today's behaviour for
 * clients that do not yet send the JWT during STOMP handshake. The connection
 * is **never** rejected here — auth failures degrade gracefully into "no
 * targeted broadcasts for this session", which is exactly what we want for
 * a no-downtime rollout.
 *
 * Errors during JWT parsing are absorbed and only logged at DEBUG; we do not
 * want a malformed token to surface as a STOMP CONNECT failure for paying
 * clients who will inevitably hit transient validation hiccups.
 */
@Component
class StompJwtAuthInterceptor(
    private val jwtService: JwtService,
    private val userDetailsService: UserDetailsService
) : ChannelInterceptor {

    private val logger = LoggerFactory.getLogger(javaClass)

    override fun preSend(message: Message<*>, channel: MessageChannel): Message<*> {
        val accessor = StompHeaderAccessor.wrap(message)
        if (accessor.command != StompCommand.CONNECT) return message

        val authHeader = accessor.getFirstNativeHeader("Authorization")
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            logger.debug("STOMP CONNECT without Bearer token — anonymous session")
            return message
        }

        val token = authHeader.substring("Bearer ".length).trim()
        if (token.isEmpty()) {
            logger.debug("STOMP CONNECT with empty Bearer token — anonymous session")
            return message
        }

        try {
            val username = jwtService.extractUsername(token)
            val userDetails = userDetailsService.loadUserByUsername(username)
            if (!jwtService.isTokenValid(token, userDetails)) {
                logger.debug("STOMP CONNECT JWT failed validation for {} — anonymous session", username)
                return message
            }
            val auth = UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                userDetails.authorities
            )
            accessor.user = auth
            logger.debug("STOMP CONNECT authenticated user={} sessionId={}", username, accessor.sessionId)
        } catch (e: Exception) {
            // Defensive: malformed/expired token, unknown user, etc. Never
            // reject the CONNECT — fall through to anonymous so polling
            // clients keep working during the rollout.
            logger.debug("STOMP CONNECT JWT parsing failed: {} — anonymous session", e.message)
        }
        return message
    }
}
