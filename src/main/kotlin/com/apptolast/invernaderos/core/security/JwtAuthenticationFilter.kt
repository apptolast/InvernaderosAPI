package com.apptolast.invernaderos.core.security

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class JwtAuthenticationFilter(
        private val jwtService: JwtService,
        private val userDetailsService: UserDetailsService
) : OncePerRequestFilter() {

    override fun doFilterInternal(
            request: HttpServletRequest,
            response: HttpServletResponse,
            filterChain: FilterChain
    ) {
        val authHeader = request.getHeader("Authorization")

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response)
            return
        }

        val jwt = authHeader.substring(7)
        // Si el token es inválido o expirado, extractUsername podría lanzar excepción
        // Dejamos que Spring Security maneje la excepción o la atrapamos si queremos customizar
        val userEmail =
                try {
                    jwtService.extractUsername(jwt)
                } catch (e: Exception) {
                    // Token inválido
                    filterChain.doFilter(request, response)
                    return
                }

        if (SecurityContextHolder.getContext().authentication == null) {
            val userDetails = this.userDetailsService.loadUserByUsername(userEmail)

            if (jwtService.isTokenValid(jwt, userDetails)) {
                val authToken =
                        UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,
                                userDetails.authorities
                        )
                authToken.details = WebAuthenticationDetailsSource().buildDetails(request)
                SecurityContextHolder.getContext().authentication = authToken
            }
        }
        filterChain.doFilter(request, response)
    }
}
