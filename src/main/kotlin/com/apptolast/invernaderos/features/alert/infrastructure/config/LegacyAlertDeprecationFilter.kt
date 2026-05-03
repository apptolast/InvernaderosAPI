package com.apptolast.invernaderos.features.alert.infrastructure.config

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

/**
 * Adds RFC 8594 (Sunset) and RFC 9745 (Deprecation) headers to every response from the
 * legacy /api/v1/alerts endpoints. The hexagonal replacements live under
 * /api/v1/tenants/{tenantId}/alerts and are pointed to via the Link header.
 *
 * Computed once at startup so all responses carry the same Sunset value.
 */
@Component
@Order(Ordered.LOWEST_PRECEDENCE)
class LegacyAlertDeprecationFilter : OncePerRequestFilter() {

    private val sunsetDate: String = ZonedDateTime.now(ZoneOffset.UTC)
        .plusDays(90)
        .format(DateTimeFormatter.RFC_1123_DATE_TIME)

    private val linkHeader: String =
        "<https://inverapi-prod.apptolast.com/swagger-ui.html#tenant-alerts>; rel=\"successor-version\""

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        try {
            filterChain.doFilter(request, response)
        } finally {
            val uri = request.requestURI
            if (uri == "/api/v1/alerts" || uri.startsWith("/api/v1/alerts/")) {
                response.setHeader("Deprecation", "true")
                response.setHeader("Sunset", sunsetDate)
                response.setHeader("Link", linkHeader)
            }
        }
    }
}
