package com.apptolast.invernaderos.features.auth

import com.apptolast.invernaderos.core.security.JwtService
import com.apptolast.invernaderos.features.user.UserService
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.stereotype.Service

@Service
class AuthService(
        private val authenticationManager: AuthenticationManager,
        private val jwtService: JwtService,
        private val userService: UserService,
        private val userDetailsService: UserDetailsService,
        private val emailService: EmailService
) {

        fun login(request: LoginRequest): JwtResponse {
                authenticationManager.authenticate(
                        UsernamePasswordAuthenticationToken(request.username, request.password)
                )

                val userDetails = userDetailsService.loadUserByUsername(request.username)
                val user =
                        userService.findByEmail(request.username)
                                ?: throw RuntimeException(
                                        "User not found after authentication"
                                ) // Should not happen

                val extraClaims = mapOf("tenantId" to user.tenantId, "role" to user.role)

                val token = jwtService.generateToken(extraClaims, userDetails)

                return JwtResponse(
                        token = token,
                        username = userDetails.username,
                        roles = userDetails.authorities.map { it.authority }
                )
        }

        fun register(request: RegisterRequest): JwtResponse {
                if (userService.existsByEmail(request.email)) {
                        throw RuntimeException("Email already in use")
                }

                val user =
                        userService.createTenantAndAdminUser(
                                tenantName = request.companyName,
                                email = request.email,
                                passwordRaw = request.password,
                                firstName = request.firstName,
                                lastName = request.lastName,
                                phone = request.phone,
                                province = request.address
                        )

                // Auto-login after registration
                val userDetails = userDetailsService.loadUserByUsername(user.email)
                val extraClaims = mapOf("tenantId" to user.tenantId, "role" to user.role)
                val token = jwtService.generateToken(extraClaims, userDetails)

                return JwtResponse(
                        token = token,
                        username = user.email,
                        roles = listOf("ROLE_" + user.role) // Simple mapping
                )
        }

        fun forgotPassword(request: ForgotPasswordRequest) {
                val token = userService.generatePasswordResetToken(request.email)
                // In a real scenario, we might want to do this asynchronously
                emailService.sendPasswordResetEmail(request.email, token)
        }

        fun resetPassword(request: ResetPasswordRequest) {
                userService.resetPassword(request.token, request.newPassword)
        }
}
