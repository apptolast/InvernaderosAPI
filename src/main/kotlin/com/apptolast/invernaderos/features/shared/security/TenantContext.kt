package com.apptolast.invernaderos.features.shared.security

import com.apptolast.invernaderos.features.user.UserRepository
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component

/**
 * Thread-safe helper that exposes the authenticated user's tenant ID and user ID
 * without re-parsing the JWT on every call.
 *
 * [JwtAuthenticationFilter] populates `authentication.details` with:
 * ```
 * mapOf("tenantId" to <Long>, "userId" to <Long>)
 * ```
 * These helpers read from that map first; if the entry is absent (e.g. during tests
 * that do not go through the filter) they fall back to a database lookup by email.
 */
@Component
class TenantContext(private val userRepository: UserRepository) {

    /**
     * Returns the tenant ID of the currently authenticated user.
     *
     * Reads from `authentication.details["tenantId"]` (populated by the JWT filter).
     * Falls back to a database lookup when the entry is absent.
     *
     * @throws AccessDeniedException if the user is not authenticated or has no tenant.
     */
    fun currentTenantId(): Long {
        val auth = SecurityContextHolder.getContext().authentication
            ?: throw AccessDeniedException("Not authenticated")
        val details = auth.details as? Map<*, *>
        val cached = (details?.get("tenantId") as? Number)?.toLong()
        if (cached != null) return cached

        // Fallback: DB lookup (happens in test contexts without the JWT filter)
        val email = auth.name ?: throw AccessDeniedException("Not authenticated")
        return userRepository.findByEmail(email)?.tenantId
            ?: throw AccessDeniedException("User not found")
    }

    /**
     * Returns the database user ID of the currently authenticated user.
     *
     * Reads from `authentication.details["userId"]` (populated by the JWT filter).
     * Falls back to a database lookup when the entry is absent.
     *
     * @throws AccessDeniedException if the user is not authenticated or not found.
     */
    fun currentUserId(): Long {
        val auth = SecurityContextHolder.getContext().authentication
            ?: throw AccessDeniedException("Not authenticated")
        val details = auth.details as? Map<*, *>
        val cached = (details?.get("userId") as? Number)?.toLong()
        if (cached != null) return cached

        // Fallback: DB lookup
        val email = auth.name ?: throw AccessDeniedException("Not authenticated")
        return userRepository.findByEmail(email)?.id
            ?: throw AccessDeniedException("User not found")
    }
}
