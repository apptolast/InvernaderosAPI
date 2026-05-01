package com.apptolast.invernaderos.config

import com.apptolast.invernaderos.core.security.JwtService
import org.slf4j.LoggerFactory
import org.springframework.messaging.Message
import org.springframework.messaging.MessageChannel
import org.springframework.messaging.simp.stomp.StompCommand
import org.springframework.messaging.simp.stomp.StompHeaderAccessor
import org.springframework.messaging.support.ChannelInterceptor
import org.springframework.messaging.support.MessageHeaderAccessor
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
        // IMPORTANT: use MessageHeaderAccessor.getAccessor (the mutable
        // accessor Spring carries on the message) — NOT
        // StompHeaderAccessor.wrap, which creates a read-only wrapper whose
        // setUser(...) does not propagate to the message that downstream
        // interceptors see. Symptom of using `wrap` here is exactly what
        // the mobile reported: the interceptor logs success but the STOMP
        // session is anonymous from SimpUserRegistry's point of view, and
        // convertAndSendToUser silently drops the frame.
        val accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor::class.java)
            ?: return message
        if (accessor.command != StompCommand.CONNECT) return message

        val sessionId = accessor.sessionId
        val authHeader = accessor.getFirstNativeHeader("Authorization")
        // INFO (was DEBUG) so we can diagnose auth flow from Dozzle without
        // toggling levels. CONNECT runs once per STOMP session — at most a
        // handful of lines per minute even with many clients reconnecting.
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            logger.info("STOMP CONNECT no-bearer sessionId={} — anonymous session, no targeted broadcasts", sessionId)
            return message
        }

        val token = authHeader.substring("Bearer ".length).trim()
        if (token.isEmpty()) {
            logger.info("STOMP CONNECT empty-bearer sessionId={} — anonymous session", sessionId)
            return message
        }

        try {
            val username = jwtService.extractUsername(token)
            val userDetails = userDetailsService.loadUserByUsername(username)
            if (!jwtService.isTokenValid(token, userDetails)) {
                logger.warn(
                    "STOMP CONNECT invalid-token sessionId={} subject={} — anonymous session " +
                        "(token expired or signature mismatch)",
                    sessionId, username
                )
                return message
            }
            val auth = UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                userDetails.authorities
            )
            accessor.user = auth
            logger.info(
                "STOMP CONNECT authenticated sessionId={} principal={} (Principal.getName() will match this string)",
                sessionId, username
            )
        } catch (e: Exception) {
            // Defensive: malformed/expired token, unknown user, etc. Never
            // reject the CONNECT — fall through to anonymous so polling
            // clients keep working during the rollout.
            logger.warn(
                "STOMP CONNECT parse-failed sessionId={} reason={} — anonymous session",
                sessionId, e.message
            )
        }
        return message
    }
}
