package com.apptolast.invernaderos.core.config

import jakarta.servlet.Filter
import jakarta.servlet.FilterChain
import jakarta.servlet.ServletRequest
import jakarta.servlet.ServletResponse
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component

/**
 * Simple In-Memory Rate Limiting Filter. Uses a Token Bucket algorithm. Limit: 100 requests per
 * minute per IP.
 */
@Component
class RateLimitingFilter : Filter {

    private val requestCounts = ConcurrentHashMap<String, AtomicInteger>()
    private val requestTime = ConcurrentHashMap<String, Long>()

    // Configuration
    private val MAX_REQUESTS_PER_MINUTE = 100
    private val RESET_INTERVAL_MS = 60000L // 1 minute

    override fun doFilter(request: ServletRequest, response: ServletResponse, chain: FilterChain) {
        val httpRequest = request as HttpServletRequest
        val httpResponse = response as HttpServletResponse
        val clientIp = httpRequest.remoteAddr

        val currentTime = System.currentTimeMillis()

        // Reset counter if interval passed
        requestTime.compute(clientIp) { _, lastTime ->
            if (lastTime == null || currentTime - lastTime > RESET_INTERVAL_MS) {
                requestCounts[clientIp] = AtomicInteger(0)
                currentTime
            } else {
                lastTime
            }
        }

        val count = requestCounts.computeIfAbsent(clientIp) { AtomicInteger(0) }.incrementAndGet()

        if (count > MAX_REQUESTS_PER_MINUTE) {
            httpResponse.status = HttpStatus.TOO_MANY_REQUESTS.value()
            httpResponse.writer.write("Too many requests. Please try again later.")
            return
        }

        chain.doFilter(request, response)
    }
}
