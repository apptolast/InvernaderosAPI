package com.apptolast.invernaderos.core.config

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import java.util.UUID
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class RequestLoggingFilter : OncePerRequestFilter() {

    private val log = LoggerFactory.getLogger(RequestLoggingFilter::class.java)

    override fun doFilterInternal(
            request: HttpServletRequest,
            response: HttpServletResponse,
            filterChain: FilterChain
    ) {
        val requestId = UUID.randomUUID().toString()
        val startTime = System.currentTimeMillis()

        // Add Request ID to response header for tracing
        response.addHeader("X-Request-ID", requestId)

        log.info("REQUEST [{}] {} {}", requestId, request.method, request.requestURI)

        try {
            filterChain.doFilter(request, response)
        } finally {
            val duration = System.currentTimeMillis() - startTime
            log.info(
                    "RESPONSE [{}] Status: {} | Time: {} ms",
                    requestId,
                    response.status,
                    duration
            )
        }
    }
}
