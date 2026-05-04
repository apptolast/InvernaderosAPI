package com.apptolast.invernaderos.features.shared.security

/**
 * Marks a controller method that must be scoped to the authenticated user's tenant.
 *
 * The [TenantOwnershipAspect] intercepts every method carrying this annotation and
 * verifies that the `tenantId` extracted from the path variable (or, for legacy
 * endpoints that use a query param, the parameter identified by [queryParam]) matches
 * the `tenantId` stored in the authentication details by [JwtAuthenticationFilter].
 *
 * When a mismatch is detected the aspect throws [org.springframework.security.access.AccessDeniedException],
 * which is mapped to HTTP 403 by [com.apptolast.invernaderos.core.exception.GlobalExceptionHandler].
 *
 * Usage:
 * ```kotlin
 * // Path-variable variant (default)
 * @GetMapping
 * @RequiresTenantOwnership
 * fun getAllByTenantId(@PathVariable tenantId: Long): ResponseEntity<*> { ... }
 *
 * // Query-param variant (legacy AlertController)
 * @GetMapping
 * @RequiresTenantOwnership(queryParam = "tenantId")
 * fun getAlerts(@RequestParam tenantId: Long, ...): ResponseEntity<*> { ... }
 * ```
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class RequiresTenantOwnership(
    /** Name of the path-variable (or query-param) parameter that carries the tenant ID. */
    val pathVariable: String = "tenantId",
    /** When non-empty, the named query-param is used instead of [pathVariable]. */
    val queryParam: String = "",
)
