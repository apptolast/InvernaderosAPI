package com.apptolast.invernaderos.features.auth

import com.apptolast.invernaderos.core.security.JwtService
import com.apptolast.invernaderos.features.auth.dto.request.ForgotPasswordRequest
import com.apptolast.invernaderos.features.auth.dto.request.LoginRequest
import com.apptolast.invernaderos.features.auth.dto.request.RefreshRequest
import com.apptolast.invernaderos.features.auth.dto.request.RegisterRequest
import com.apptolast.invernaderos.features.auth.dto.request.ResetPasswordRequest
import com.apptolast.invernaderos.features.auth.dto.response.JwtResponse
import com.apptolast.invernaderos.features.auth.refresh.application.usecase.AuthErrorException
import com.apptolast.invernaderos.features.auth.refresh.domain.error.AuthError
import com.apptolast.invernaderos.features.user.UserService
import org.springframework.beans.factory.annotation.Autowired
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

        /** Injected after construction to avoid circular dependency and preserve 5-arg constructor for tests. */
        @Autowired(required = false)
        private var authRefreshService: AuthRefreshService? = null

        fun login(request: LoginRequest): JwtResponse {
                val delegate = authRefreshService
                if (delegate != null) {
                        return delegate.loginWithRefresh(request)
                }

                authenticationManager.authenticate(
                        UsernamePasswordAuthenticationToken(request.username, request.password)
                )

                val userDetails = userDetailsService.loadUserByUsername(request.username)
                val user =
                        userService.findByEmail(request.username)
                                ?: throw RuntimeException(
                                        "User not found after authentication"
                                )

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

                val delegate = authRefreshService
                if (delegate != null) {
                        return delegate.registerWithRefresh(request)
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

                val userDetails = userDetailsService.loadUserByUsername(user.email)
                val extraClaims = mapOf("tenantId" to user.tenantId, "role" to user.role)
                val token = jwtService.generateToken(extraClaims, userDetails)

                return JwtResponse(
                        token = token,
                        username = user.email,
                        roles = listOf("ROLE_" + user.role)
                )
        }

        fun refresh(req: RefreshRequest): Result<JwtResponse> {
                val delegate = authRefreshService
                        ?: return Result.failure(AuthErrorException(AuthError.FeatureDisabled))
                return delegate.refresh(req)
        }

        fun forgotPassword(request: ForgotPasswordRequest) {
                val token = userService.generatePasswordResetToken(request.email)
                emailService.sendPasswordResetEmail(request.email, token)
        }

        fun resetPassword(request: ResetPasswordRequest) {
                userService.resetPassword(request.token, request.newPassword)
        }
}
