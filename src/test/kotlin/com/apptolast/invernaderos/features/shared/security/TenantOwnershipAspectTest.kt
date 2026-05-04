package com.apptolast.invernaderos.features.shared.security

import io.mockk.every
import io.mockk.mockk
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.reflect.MethodSignature
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder

/**
 * Unit tests for [TenantOwnershipAspect].
 *
 * Uses stubbed [ProceedingJoinPoint] and [SecurityContextHolder] to verify
 * allow / deny behaviour without a Spring context.
 */
class TenantOwnershipAspectTest {

    private val aspect = TenantOwnershipAspect()
    private val annotation = RequiresTenantOwnership()

    @BeforeEach
    fun clearSecurityContext() {
        SecurityContextHolder.clearContext()
    }

    // -----------------------------------------------------------------------
    // Helper builders
    // -----------------------------------------------------------------------

    private fun buildJoinPoint(paramNames: Array<String>, args: Array<Any?>): ProceedingJoinPoint {
        val signature = mockk<MethodSignature>()
        every { signature.parameterNames } returns paramNames
        every { signature.declaringTypeName } returns "TestController"
        every { signature.name } returns "testMethod"
        val joinPoint = mockk<ProceedingJoinPoint>()
        every { joinPoint.signature } returns signature
        every { joinPoint.args } returns args
        every { joinPoint.proceed() } returns Unit
        return joinPoint
    }

    private fun setAuthWithTenantId(tenantId: Long) {
        val auth = UsernamePasswordAuthenticationToken("user@example.com", null, emptyList())
        auth.details = mapOf("tenantId" to tenantId, "userId" to 99L)
        SecurityContextHolder.getContext().authentication = auth
    }

    // -----------------------------------------------------------------------
    // Tests
    // -----------------------------------------------------------------------

    @Test
    fun `should allow when jwt tenantId matches path tenantId`() {
        setAuthWithTenantId(42L)
        val joinPoint = buildJoinPoint(arrayOf("tenantId", "alertId"), arrayOf(42L, 1L))

        // Should NOT throw
        aspect.enforce(joinPoint, annotation)
    }

    @Test
    fun `should deny when jwt tenantId does not match path tenantId`() {
        setAuthWithTenantId(42L)
        val joinPoint = buildJoinPoint(arrayOf("tenantId", "alertId"), arrayOf(99L, 1L))

        assertThrows<AccessDeniedException> {
            aspect.enforce(joinPoint, annotation)
        }
    }

    @Test
    fun `should deny when authentication has no tenantId in details`() {
        val auth = UsernamePasswordAuthenticationToken("user@example.com", null, emptyList())
        // details is null — no tenant info in context
        SecurityContextHolder.getContext().authentication = auth

        val joinPoint = buildJoinPoint(arrayOf("tenantId"), arrayOf(42L))

        assertThrows<AccessDeniedException> {
            aspect.enforce(joinPoint, annotation)
        }
    }

    @Test
    fun `should skip check and proceed when tenantId param is not found`() {
        setAuthWithTenantId(42L)
        // Method has no 'tenantId' parameter
        val joinPoint = buildJoinPoint(arrayOf("alertId"), arrayOf(1L))

        aspect.enforce(joinPoint, annotation)
        // No exception — aspect logs a warning and proceeds
    }

    @Test
    fun `should use queryParam name when annotation specifies queryParam`() {
        setAuthWithTenantId(7L)
        val queryParamAnnotation = RequiresTenantOwnership(queryParam = "tenantId")
        val joinPoint = buildJoinPoint(arrayOf("tenantId", "sectorId"), arrayOf(7L, 5L))

        // Should NOT throw
        aspect.enforce(joinPoint, queryParamAnnotation)
    }

    @Test
    fun `should deny cross-tenant access for queryParam variant`() {
        setAuthWithTenantId(7L)
        val queryParamAnnotation = RequiresTenantOwnership(queryParam = "tenantId")
        val joinPoint = buildJoinPoint(arrayOf("tenantId", "sectorId"), arrayOf(99L, 5L))

        val ex = assertThrows<AccessDeniedException> {
            aspect.enforce(joinPoint, queryParamAnnotation)
        }
        assertEquals("cross-tenant access denied", ex.message)
    }
}
