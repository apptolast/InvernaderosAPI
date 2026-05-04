package com.apptolast.invernaderos.features.shared.security

import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.reflect.MethodSignature
import org.slf4j.LoggerFactory
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component

/**
 * AOP aspect that enforces cross-tenant isolation for all controller methods
 * annotated with [RequiresTenantOwnership].
 *
 * ## How it works
 * 1. The aspect intercepts any method annotated with `@RequiresTenantOwnership`.
 * 2. It extracts the `tenantId` argument from the method's parameter list using the
 *    name specified in the annotation (`pathVariable` or `queryParam`).
 * 3. It reads the authenticated user's `tenantId` from `authentication.details`,
 *    which was populated by [JwtAuthenticationFilter] when the JWT was validated.
 * 4. If the two tenant IDs do not match, it throws [AccessDeniedException], which
 *    is mapped to HTTP 403 by the global exception handler.
 *
 * ## Why AOP rather than Spring Security `@PreAuthorize`
 * `@PreAuthorize` with SpEL cannot safely compare a path-variable Long with a claim
 * Long without custom Security expression roots, which would require additional bean
 * wiring. An explicit `@Aspect` keeps the logic self-contained and testable.
 */
@Aspect
@Component
class TenantOwnershipAspect {

    private val log = LoggerFactory.getLogger(javaClass)

    @Around("@annotation(requiresTenantOwnership)")
    fun enforce(joinPoint: ProceedingJoinPoint, requiresTenantOwnership: RequiresTenantOwnership): Any? {
        // Admins (e.g. the GreenhouseAdmin portal) need cross-tenant read/write access.
        // Bypass the ownership check when the principal carries ROLE_ADMIN; regular users
        // remain strictly tenant-isolated below.
        if (hasAdminAuthority()) {
            return joinPoint.proceed()
        }

        val paramName = requiresTenantOwnership.queryParam
            .ifEmpty { requiresTenantOwnership.pathVariable }

        val tenantIdFromPath: Long? = extractTenantIdArg(joinPoint, paramName)
        if (tenantIdFromPath == null) {
            log.debug(
                "TenantOwnershipAspect: no param named '{}' found on {}.{} — rejecting",
                paramName, joinPoint.signature.declaringTypeName, joinPoint.signature.name,
            )
            throw AccessDeniedException("cross-tenant access denied")
        }

        val jwtTenantId = extractTenantIdFromAuth()
        if (jwtTenantId == null) {
            log.debug("TenantOwnershipAspect: no tenantId in authentication details — rejecting")
            throw AccessDeniedException("cross-tenant access denied")
        }

        if (jwtTenantId != tenantIdFromPath) {
            log.debug(
                "TenantOwnershipAspect: cross-tenant access denied — jwt.tenantId={} path.tenantId={}",
                jwtTenantId, tenantIdFromPath,
            )
            throw AccessDeniedException("cross-tenant access denied")
        }

        return joinPoint.proceed()
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private fun extractTenantIdArg(joinPoint: ProceedingJoinPoint, paramName: String): Long? {
        val signature = joinPoint.signature as? MethodSignature ?: return null
        val paramNames = signature.parameterNames ?: return null
        val index = paramNames.indexOf(paramName)
        if (index < 0 || index >= joinPoint.args.size) return null
        return when (val arg = joinPoint.args[index]) {
            is Long -> arg
            is Number -> arg.toLong()
            else -> null
        }
    }

    private fun extractTenantIdFromAuth(): Long? {
        val auth = SecurityContextHolder.getContext().authentication ?: return null
        val details = auth.details as? Map<*, *> ?: return null
        return (details["tenantId"] as? Number)?.toLong()
    }

    private fun hasAdminAuthority(): Boolean {
        val auth = SecurityContextHolder.getContext().authentication ?: return false
        return auth.authorities?.any { it.authority == "ROLE_ADMIN" } == true
    }
}
