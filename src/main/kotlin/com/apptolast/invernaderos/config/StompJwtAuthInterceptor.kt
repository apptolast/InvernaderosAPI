package com.apptolast.invernaderos.config

import com.apptolast.invernaderos.core.security.JwtService
import org.slf4j.LoggerFactory
import org.springframework.messaging.Message
import org.springframework.messaging.MessageChannel
import org.springframework.messaging.simp.stomp.StompCommand
import org.springframework.messaging.simp.stomp.StompHeaderAccessor
import org.springframework.messaging.support.ChannelInterceptor
import org.springframework.messaging.support.MessageHeaderAccessor
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.stereotype.Component

/**
 * STOMP authentication interceptor.
 *
 * Hard-fails any STOMP CONNECT that does not carry a valid `Authorization:
 * Bearer <jwt>` native header. On any of {missing header, malformed header,
 * empty token, parse error, signature mismatch, expired, user not found,
 * inactive user} this interceptor throws [AccessDeniedException] from
 * [preSend]. Spring Messaging propagates the exception to the client as a
 * STOMP ERROR frame and closes the session — no silent anonymous fallback.
 *
 * Once the CONNECT is accepted, the authenticated principal is attached to
 * the session via [StompHeaderAccessor.setUser] using the message's mutable
 * accessor. This is required so that
 * [org.springframework.messaging.simp.SimpMessagingTemplate.convertAndSendToUser]
 * can target the session by principal name (the user's email, matching
 * `User.email` and what
 * [com.apptolast.invernaderos.core.security.CustomUserDetailsService] returns
 * as `userDetails.username`).
 *
 * SEND and SUBSCRIBE frames are also gated: if the session somehow reaches
 * those commands without a bound principal we throw. Defense in depth
 * against future Spring changes that might let an unauthenticated session
 * past CONNECT.
 *
 * **Why** the previous "backwards-compatible anonymous fallback" was
 * removed: it allowed clients without a Bearer to subscribe to
 * `/user/queue/...` and trigger handlers that do not check the principal,
 * exposing every active tenant's snapshot. See plan
 * `bright-stirring-starfish.md`.
 */
@Component
class StompJwtAuthInterceptor(
    private val jwtService: JwtService,
    private val userDetailsService: UserDetailsService
) : ChannelInterceptor {

    private val logger = LoggerFactory.getLogger(javaClass)

    override fun preSend(message: Message<*>, channel: MessageChannel): Message<*> {
        // Use the mutable accessor Spring carries on the message.
        // StompHeaderAccessor.wrap returns a read-only wrapper whose
        // setUser(...) does not propagate downstream; using `wrap` here
        // is the bug that makes SimpUserRegistry see anonymous sessions
        // even when this interceptor logs success.
        val accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor::class.java)
            ?: return message

        return when (accessor.command) {
            StompCommand.CONNECT -> authenticateConnect(message, accessor)
            StompCommand.SEND, StompCommand.SUBSCRIBE -> requireAuthenticated(message, accessor)
            else -> message
        }
    }

    private fun authenticateConnect(message: Message<*>, accessor: StompHeaderAccessor): Message<*> {
        val sessionId = accessor.sessionId
        val authHeader = accessor.getFirstNativeHeader("Authorization")

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            logger.info("STOMP CONNECT rejected sessionId={} reason=no-bearer", sessionId)
            throw AccessDeniedException("STOMP CONNECT requires Authorization: Bearer <jwt>")
        }

        val token = authHeader.substring("Bearer ".length).trim()
        if (token.isEmpty()) {
            logger.info("STOMP CONNECT rejected sessionId={} reason=empty-bearer", sessionId)
            throw AccessDeniedException("STOMP CONNECT bearer token is empty")
        }

        val username = try {
            jwtService.extractUsername(token)
        } catch (e: Exception) {
            logger.warn("STOMP CONNECT rejected sessionId={} reason=parse-failed: {}", sessionId, e.message)
            throw AccessDeniedException("STOMP CONNECT bearer token is malformed")
        }

        val userDetails = try {
            userDetailsService.loadUserByUsername(username)
        } catch (e: Exception) {
            // Includes UsernameNotFoundException and "User is not active"
            // (CustomUserDetailsService throws UsernameNotFoundException for both).
            logger.warn(
                "STOMP CONNECT rejected sessionId={} subject={} reason=user-load-failed: {}",
                sessionId, username, e.message
            )
            throw AccessDeniedException("STOMP CONNECT user not found or inactive")
        }

        if (!jwtService.isTokenValid(token, userDetails)) {
            logger.warn(
                "STOMP CONNECT rejected sessionId={} subject={} reason=invalid-or-expired",
                sessionId, username
            )
            throw AccessDeniedException("STOMP CONNECT bearer token is invalid or expired")
        }

        val auth = UsernamePasswordAuthenticationToken(userDetails, null, userDetails.authorities)
        accessor.user = auth
        logger.info(
            "STOMP CONNECT accepted sessionId={} principal={} authorities={}",
            sessionId, username, userDetails.authorities.map { it.authority }
        )
        return message
    }

    private fun requireAuthenticated(message: Message<*>, accessor: StompHeaderAccessor): Message<*> {
        if (accessor.user == null) {
            logger.warn(
                "STOMP {} rejected sessionId={} reason=no-principal-bound",
                accessor.command, accessor.sessionId
            )
            throw AccessDeniedException("STOMP ${accessor.command} requires authenticated session")
        }
        return message
    }
}
