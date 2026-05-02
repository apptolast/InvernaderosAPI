package com.apptolast.invernaderos.features.auth

import com.apptolast.invernaderos.core.security.JwtService
import com.apptolast.invernaderos.features.auth.dto.mapper.toJwtResponse
import com.apptolast.invernaderos.features.auth.dto.request.LoginRequest
import com.apptolast.invernaderos.features.auth.dto.request.RefreshRequest
import com.apptolast.invernaderos.features.auth.dto.request.RegisterRequest
import com.apptolast.invernaderos.features.auth.dto.response.JwtResponse
import com.apptolast.invernaderos.features.auth.refresh.application.usecase.AuthErrorException
import com.apptolast.invernaderos.features.auth.refresh.domain.error.AuthError
import com.apptolast.invernaderos.features.auth.refresh.domain.port.input.IssueRefreshTokenCommand
import com.apptolast.invernaderos.features.auth.refresh.domain.port.input.IssueRefreshTokenUseCase
import com.apptolast.invernaderos.features.auth.refresh.domain.port.input.RotateRefreshTokenUseCase
import com.apptolast.invernaderos.features.user.UserService
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.stereotype.Service

/**
 * Extends authentication with opaque refresh-token rotation.
 * Wraps [AuthService] for login/register with refresh-token issuance,
 * and provides the /refresh endpoint operation.
 *
 * This service is intentionally separate from [AuthService] so that
 * existing unit tests for [AuthService] do not require mocking of the
 * refresh-token use cases.
 */
@Service
class AuthRefreshService(
        private val authenticationManager: AuthenticationManager,
        private val jwtService: JwtService,
        private val userService: UserService,
        private val userDetailsService: UserDetailsService,
        private val issueRefreshTokenUseCase: IssueRefreshTokenUseCase,
        private val rotateRefreshTokenUseCase: RotateRefreshTokenUseCase,
        @Value("\${auth.refresh-token.enabled:true}") private val refreshTokenEnabled: Boolean
) {

        fun loginWithRefresh(request: LoginRequest): JwtResponse {
                authenticationManager.authenticate(
                        UsernamePasswordAuthenticationToken(request.username, request.password)
                )

                val userDetails = userDetailsService.loadUserByUsername(request.username)
                val user = userService.findByEmail(request.username)
                        ?: throw IllegalStateException("User not found after authentication")
                val userId = user.id
                        ?: throw IllegalStateException("Persisted user ${user.email} has null id")

                return if (refreshTokenEnabled) {
                        val tokens = issueRefreshTokenUseCase.execute(
                                IssueRefreshTokenCommand(userId = userId)
                        )
                        tokens.toJwtResponse()
                } else {
                        val extraClaims = mapOf("tenantId" to user.tenantId, "role" to user.role)
                        val token = jwtService.generateToken(extraClaims, userDetails)
                        JwtResponse(
                                token = token,
                                username = userDetails.username,
                                roles = userDetails.authorities.map { it.authority }
                        )
                }
        }

        fun registerWithRefresh(request: RegisterRequest): JwtResponse {
                val user = userService.createTenantAndAdminUser(
                        tenantName = request.companyName,
                        email = request.email,
                        passwordRaw = request.password,
                        firstName = request.firstName,
                        lastName = request.lastName,
                        phone = request.phone,
                        province = request.address
                )

                return if (refreshTokenEnabled) {
                        val savedUser = userService.findByEmail(user.email)
                                ?: throw IllegalStateException("User not found after registration")
                        val savedUserId = savedUser.id
                                ?: throw IllegalStateException("Persisted user ${savedUser.email} has null id")
                        val tokens = issueRefreshTokenUseCase.execute(
                                IssueRefreshTokenCommand(userId = savedUserId)
                        )
                        tokens.toJwtResponse()
                } else {
                        val userDetails = userDetailsService.loadUserByUsername(user.email)
                        val extraClaims = mapOf("tenantId" to user.tenantId, "role" to user.role)
                        val token = jwtService.generateToken(extraClaims, userDetails)
                        JwtResponse(
                                token = token,
                                username = user.email,
                                roles = listOf("ROLE_" + user.role)
                        )
                }
        }

        fun refresh(req: RefreshRequest): Result<JwtResponse> {
                if (!refreshTokenEnabled) {
                        return Result.failure(AuthErrorException(AuthError.FeatureDisabled))
                }
                return rotateRefreshTokenUseCase.execute(req.refreshToken)
                        .map { it.toJwtResponse() }
        }
}
