# Security Recommendations for Production Deployment

## Overview
This document provides security recommendations for the InvernaderosAPI based on the comprehensive code review. The application is well-architected but requires security hardening before production deployment.

**Current Security Status:** ⚠️ Development Mode (NOT production-ready)

---

## 🔴 HIGH PRIORITY - Critical Security Issues

### 1. Enable Spring Security

**Current State:**
```kotlin
// build.gradle.kts - Spring Security is commented out
//implementation("org.springframework.boot:spring-boot-starter-security")
```

**Recommendation:**
1. **Uncomment Spring Security dependency**
2. **Add basic authentication/authorization**
3. **Secure all API endpoints**

**Implementation Steps:**

#### Step 1: Enable Spring Security
```kotlin
// build.gradle.kts
dependencies {
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.security:spring-security-config")
}
```

#### Step 2: Create Security Configuration
```kotlin
// src/main/kotlin/.../config/SecurityConfig.kt
package com.apptolast.invernaderos.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder

@Configuration
@EnableWebSecurity
class SecurityConfig {
    
    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { csrf -> csrf.disable() } // For REST APIs with token auth
            .sessionManagement { session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            }
            .authorizeHttpRequests { auth ->
                auth
                    // Public endpoints
                    .requestMatchers("/actuator/health").permitAll()
                    .requestMatchers("/api/greenhouse/health").permitAll()
                    
                    // Swagger/OpenAPI (development only)
                    .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                    
                    // Require authentication for all other endpoints
                    .anyRequest().authenticated()
            }
            .httpBasic { } // Basic auth for simplicity, consider JWT for production
        
        return http.build()
    }
    
    @Bean
    fun passwordEncoder(): PasswordEncoder {
        return BCryptPasswordEncoder()
    }
}
```

#### Step 3: Multi-Tenant Security
```kotlin
// Add tenant-based authorization
@Component
class TenantAuthorizationService {
    
    fun hasAccessToTenant(userId: UUID, tenantId: UUID): Boolean {
        // Check if user belongs to tenant
        // Query user-tenant relationship from database
        return true // Implement actual logic
    }
    
    fun hasAccessToGreenhouse(userId: UUID, greenhouseId: UUID): Boolean {
        // Check if user has access to specific greenhouse
        return true // Implement actual logic
    }
}

// Use in service layer
@Service
class AlertService(
    private val alertRepository: AlertRepository,
    private val tenantAuthService: TenantAuthorizationService
) {
    
    @Transactional("postgreSQLTransactionManager", readOnly = true)
    fun getAllByTenant(tenantId: UUID): List<Alert> {
        // Verify current user has access to tenant
        val currentUserId = SecurityContextHolder.getContext().authentication.name
        if (!tenantAuthService.hasAccessToTenant(UUID.fromString(currentUserId), tenantId)) {
            throw AccessDeniedException("User does not have access to tenant: $tenantId")
        }
        
        return alertRepository.findByTenantId(tenantId)
    }
}
```

#### Step 4: JWT Token Authentication (Recommended for Production)
```kotlin
// build.gradle.kts
dependencies {
    implementation("io.jsonwebtoken:jjwt-api:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.6")
}

// JwtTokenProvider.kt
@Component
class JwtTokenProvider {
    
    @Value("\${jwt.secret}")
    private lateinit var jwtSecret: String
    
    @Value("\${jwt.expiration:3600000}") // 1 hour
    private var jwtExpiration: Long = 3600000
    
    fun generateToken(userId: UUID, tenantId: UUID): String {
        val claims = mapOf(
            "userId" to userId.toString(),
            "tenantId" to tenantId.toString()
        )
        
        return Jwts.builder()
            .claims(claims)
            .subject(userId.toString())
            .issuedAt(Date())
            .expiration(Date(System.currentTimeMillis() + jwtExpiration))
            .signWith(Keys.hmacShaKeyFor(jwtSecret.toByteArray()))
            .compact()
    }
    
    fun validateToken(token: String): Boolean {
        try {
            Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(jwtSecret.toByteArray()))
                .build()
                .parseSignedClaims(token)
            return true
        } catch (e: JwtException) {
            return false
        }
    }
    
    fun getUserIdFromToken(token: String): UUID {
        val claims = Jwts.parser()
            .verifyWith(Keys.hmacShaKeyFor(jwtSecret.toByteArray()))
            .build()
            .parseSignedClaims(token)
            .payload
        
        return UUID.fromString(claims.subject)
    }
}
```

---

### 2. Restrict CORS Origins

**Current State:**
```kotlin
// All controllers have wide-open CORS
@CrossOrigin(origins = ["*"])
```

**Recommendation:**
1. **Remove `@CrossOrigin` from controllers**
2. **Configure CORS globally**
3. **Restrict to specific origins**

**Implementation:**

```kotlin
// src/main/kotlin/.../config/WebConfig.kt
package com.apptolast.invernaderos.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class WebConfig : WebMvcConfigurer {
    
    @Value("\${cors.allowed-origins}")
    private lateinit var allowedOrigins: String
    
    override fun addCorsMappings(registry: CorsRegistry) {
        registry.addMapping("/api/**")
            .allowedOrigins(*allowedOrigins.split(",").toTypedArray())
            .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
            .allowedHeaders("*")
            .allowCredentials(true)
            .maxAge(3600)
    }
}
```

```yaml
# application.yaml
cors:
  allowed-origins: ${ALLOWED_ORIGINS:http://localhost:3000,http://localhost:5173}
```

```yaml
# application-prod.yaml
cors:
  allowed-origins: https://yourdomain.com,https://www.yourdomain.com
```

**Then remove from controllers:**
```kotlin
@RestController
@RequestMapping("/api/alerts")
// @CrossOrigin(origins = ["*"]) <- REMOVE THIS
class AlertController(...)
```

---

### 3. Input Validation & Sanitization

**Current State:** ✅ Bean Validation is used (good start)

**Recommendations:**

#### Add More Validation Constraints
```kotlin
// Alert entity - add validation
import jakarta.validation.constraints.*

@Entity
data class Alert(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    
    @field:NotNull(message = "Tenant ID is required")
    @Column(name = "tenant_id", nullable = false)
    val tenantId: UUID,
    
    @field:NotNull(message = "Greenhouse ID is required")
    @Column(name = "greenhouse_id", nullable = false)
    val greenhouseId: UUID,
    
    @field:NotBlank(message = "Alert type is required")
    @field:Size(max = 50, message = "Alert type must not exceed 50 characters")
    @field:Pattern(
        regexp = "^(THRESHOLD_EXCEEDED|SENSOR_OFFLINE|ACTUATOR_FAILURE|SYSTEM_ERROR|DATA_QUALITY|CONNECTIVITY)$",
        message = "Invalid alert type"
    )
    @Column(name = "alert_type", length = 50, nullable = false)
    val alertType: String,
    
    @field:NotBlank(message = "Severity is required")
    @field:Pattern(
        regexp = "^(INFO|WARNING|ERROR|CRITICAL)$",
        message = "Invalid severity level"
    )
    @Column(name = "severity", length = 20, nullable = false)
    val severity: String,
    
    @field:NotBlank(message = "Message is required")
    @field:Size(max = 5000, message = "Message must not exceed 5000 characters")
    @Column(name = "message", columnDefinition = "TEXT", nullable = false)
    val message: String,
    
    // ... rest of fields
)
```

#### Custom Validators
```kotlin
// UUID validation
@Target(AnnotationTarget.FIELD, AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = [UUIDValidator::class])
annotation class ValidUUID(
    val message: String = "Invalid UUID format",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = []
)

class UUIDValidator : ConstraintValidator<ValidUUID, UUID?> {
    override fun isValid(value: UUID?, context: ConstraintValidatorContext?): Boolean {
        return value != null
    }
}
```

---

## 🟡 MEDIUM PRIORITY - Important Security Improvements

### 4. SQL Injection Protection

**Current State:** ✅ Using JPA/JDBC parameterized queries (good!)

**Verification:**
```kotlin
// ✅ SAFE - Parameterized query
@Query("SELECT a FROM Alert a WHERE a.tenantId = :tenantId")
fun findByTenantId(@Param("tenantId") tenantId: UUID): List<Alert>

// ✅ SAFE - Native query with parameters
@Query(value = "SELECT * FROM alerts WHERE tenant_id = :tenantId", nativeQuery = true)
fun findByTenantIdNative(@Param("tenantId") tenantId: UUID): List<Alert>

// ❌ UNSAFE - String concatenation (NOT FOUND IN CODEBASE - GOOD!)
// val query = "SELECT * FROM alerts WHERE tenant_id = '$tenantId'" <- NEVER DO THIS
```

**No changes needed** - Current implementation is secure.

---

### 5. Rate Limiting

**Recommendation:** Add rate limiting to prevent abuse

```kotlin
// build.gradle.kts
dependencies {
    implementation("com.bucket4j:bucket4j-core:8.10.1")
    implementation("com.github.vladimir-bukhtoyarov:bucket4j-jcache:8.10.1")
}

// RateLimitFilter.kt
@Component
@Order(1)
class RateLimitFilter : OncePerRequestFilter() {
    
    private val buckets = ConcurrentHashMap<String, Bucket>()
    
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val clientId = getClientId(request)
        val bucket = buckets.computeIfAbsent(clientId) { createBucket() }
        
        if (bucket.tryConsume(1)) {
            filterChain.doFilter(request, response)
        } else {
            response.status = 429 // Too Many Requests
            response.writer.write("Rate limit exceeded")
        }
    }
    
    private fun getClientId(request: HttpServletRequest): String {
        // Use user ID if authenticated, otherwise IP address
        val auth = SecurityContextHolder.getContext().authentication
        return if (auth?.isAuthenticated == true) {
            auth.name
        } else {
            request.remoteAddr
        }
    }
    
    private fun createBucket(): Bucket {
        val refill = Refill.intervally(100, Duration.ofMinutes(1))
        val bandwidth = Bandwidth.classic(100, refill)
        return Bucket.builder()
            .addLimit(bandwidth)
            .build()
    }
}
```

---

### 6. Sensitive Data Protection

**Current State:** Passwords/secrets in User entity need encryption

**Recommendations:**

```kotlin
// User entity
@Entity
data class User(
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    val id: UUID? = null,
    
    @Column(unique = true, nullable = false)
    val username: String,
    
    @Column(unique = true, nullable = false)
    val email: String,
    
    // ✅ Password should be hashed with BCrypt
    @JsonIgnore // Don't expose in API responses
    @Column(nullable = false)
    val passwordHash: String,
    
    // ... other fields
)

// UserService
@Service
class UserService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder
) {
    
    @Transactional
    fun createUser(username: String, email: String, rawPassword: String): User {
        val passwordHash = passwordEncoder.encode(rawPassword)
        val user = User(
            username = username,
            email = email,
            passwordHash = passwordHash
        )
        return userRepository.save(user)
    }
    
    fun validatePassword(user: User, rawPassword: String): Boolean {
        return passwordEncoder.matches(rawPassword, user.passwordHash)
    }
}
```

---

### 7. HTTPS/TLS Configuration

**Recommendation:** Enforce HTTPS in production

```yaml
# application-prod.yaml
server:
  port: 8443
  ssl:
    enabled: true
    key-store: classpath:keystore.p12
    key-store-password: ${KEYSTORE_PASSWORD}
    key-store-type: PKCS12
    key-alias: invernaderos
    
# Redirect HTTP to HTTPS
security:
  require-ssl: true
```

```kotlin
// Redirect HTTP to HTTPS
@Configuration
class HttpsRedirectConfig {
    
    @Bean
    fun servletContainer(): ServletWebServerFactory {
        val tomcat = TomcatServletWebServerFactory()
        tomcat.addAdditionalTomcatConnectors(createHttpConnector())
        return tomcat
    }
    
    private fun createHttpConnector(): Connector {
        val connector = Connector(TomcatServletWebServerFactory.DEFAULT_PROTOCOL)
        connector.scheme = "http"
        connector.port = 8080
        connector.secure = false
        connector.redirectPort = 8443
        return connector
    }
}
```

---

## 🟢 LOW PRIORITY - Best Practice Improvements

### 8. Audit Logging

**Current State:** Basic logging exists

**Recommendation:** Add security audit trail

```kotlin
// AuditLog entity already exists - use it!
@Service
class AuditService(
    private val auditLogRepository: AuditLogRepository
) {
    
    fun logSecurityEvent(
        userId: UUID,
        action: String,
        resource: String,
        success: Boolean,
        details: String? = null
    ) {
        val auditLog = AuditLog(
            userId = userId,
            action = action,
            resource = resource,
            success = success,
            details = details,
            timestamp = Instant.now()
        )
        auditLogRepository.save(auditLog)
    }
}

// Use in services
@Service
class AlertService(
    private val alertRepository: AlertRepository,
    private val auditService: AuditService
) {
    
    @Transactional("postgreSQLTransactionManager")
    fun delete(id: Long): Boolean {
        val userId = getCurrentUserId()
        val success = alertRepository.existsById(id)
        
        if (success) {
            alertRepository.deleteById(id)
            auditService.logSecurityEvent(
                userId = userId,
                action = "DELETE_ALERT",
                resource = "Alert:$id",
                success = true
            )
        } else {
            auditService.logSecurityEvent(
                userId = userId,
                action = "DELETE_ALERT",
                resource = "Alert:$id",
                success = false,
                details = "Alert not found"
            )
        }
        
        return success
    }
}
```

---

### 9. API Versioning

**Recommendation:** Version your APIs

```kotlin
// v1/AlertController.kt
@RestController
@RequestMapping("/api/v1/alerts")
class AlertControllerV1(...)

// v2/AlertController.kt (future)
@RestController
@RequestMapping("/api/v2/alerts")
class AlertControllerV2(...)
```

---

### 10. Security Headers

**Recommendation:** Add security headers

```kotlin
@Configuration
class SecurityHeadersConfig {
    
    @Bean
    fun securityHeadersFilter(): FilterRegistrationBean<SecurityHeadersFilter> {
        val registration = FilterRegistrationBean<SecurityHeadersFilter>()
        registration.filter = SecurityHeadersFilter()
        registration.addUrlPatterns("/api/*")
        return registration
    }
}

class SecurityHeadersFilter : OncePerRequestFilter() {
    
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        // Prevent clickjacking
        response.setHeader("X-Frame-Options", "DENY")
        
        // Prevent MIME sniffing
        response.setHeader("X-Content-Type-Options", "nosniff")
        
        // XSS protection
        response.setHeader("X-XSS-Protection", "1; mode=block")
        
        // HSTS (HTTPS only)
        response.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains")
        
        // Content Security Policy
        response.setHeader("Content-Security-Policy", "default-src 'self'")
        
        filterChain.doFilter(request, response)
    }
}
```

---

## Implementation Roadmap

### Phase 1: Critical Security (Week 1)
- [ ] Enable Spring Security
- [ ] Configure basic authentication
- [ ] Restrict CORS origins
- [ ] Add multi-tenant authorization
- [ ] Configure HTTPS/TLS

### Phase 2: Enhanced Security (Week 2)
- [ ] Implement JWT authentication
- [ ] Add rate limiting
- [ ] Enhance input validation
- [ ] Configure security headers
- [ ] Add audit logging

### Phase 3: Hardening (Week 3)
- [ ] Security penetration testing
- [ ] Dependency vulnerability scanning
- [ ] API versioning
- [ ] Documentation updates
- [ ] Security training for team

---

## Testing Security

### Security Test Checklist

```kotlin
// SecurityTests.kt
@SpringBootTest
@AutoConfigureMockMvc
class SecurityTests {
    
    @Autowired
    private lateinit var mockMvc: MockMvc
    
    @Test
    fun `unauthenticated request should return 401`() {
        mockMvc.perform(get("/api/alerts"))
            .andExpect(status().isUnauthorized)
    }
    
    @Test
    fun `authenticated request with wrong tenant should return 403`() {
        // Test tenant isolation
    }
    
    @Test
    fun `SQL injection attempt should be blocked`() {
        // Test SQL injection prevention
    }
    
    @Test
    fun `XSS attempt should be sanitized`() {
        // Test XSS prevention
    }
}
```

---

## References

- [OWASP Top 10 2021](https://owasp.org/Top10/)
- [Spring Security Documentation](https://docs.spring.io/spring-security/reference/)
- [NIST Cybersecurity Framework](https://www.nist.gov/cyberframework)
- [CWE Top 25 Software Weaknesses](https://cwe.mitre.org/top25/)

---

## Compliance Considerations

### GDPR (EU)
- Implement data encryption at rest and in transit
- Add user consent management
- Implement right to erasure (delete user data)
- Add data export functionality
- Maintain audit logs

### CCPA (California)
- Add opt-out mechanisms
- Implement data deletion
- Provide data access APIs
- Maintain privacy notices

---

## Contact Security Team

For security concerns or questions:
- Email: security@yourdomain.com
- Bug Bounty: https://yourdomain.com/security
- Responsible Disclosure Policy: See SECURITY.md

---

**Document Status:** DRAFT  
**Last Updated:** 2025-11-17  
**Next Review:** After Phase 1 completion  
**Owner:** Security Team / DevOps
