package com.apptolast.invernaderos.features.auth

import com.apptolast.invernaderos.features.auth.dto.request.ForgotPasswordRequest
import com.apptolast.invernaderos.features.auth.dto.request.LoginRequest
import com.apptolast.invernaderos.features.auth.dto.request.RefreshRequest
import com.apptolast.invernaderos.features.auth.dto.request.RegisterRequest
import com.apptolast.invernaderos.features.auth.dto.request.ResetPasswordRequest
import com.apptolast.invernaderos.features.auth.dto.response.JwtResponse
import com.apptolast.invernaderos.features.user.UserService
import org.springframework.stereotype.Service

@Service
class AuthService(
        private val userService: UserService,
        private val emailService: EmailService,
        private val authRefreshService: AuthRefreshService
) {

        fun login(request: LoginRequest): JwtResponse =
                authRefreshService.loginWithRefresh(request)

        fun register(request: RegisterRequest): JwtResponse {
                if (userService.existsByEmail(request.email)) {
                        throw RuntimeException("Email already in use")
                }
                return authRefreshService.registerWithRefresh(request)
        }

        fun refresh(req: RefreshRequest): Result<JwtResponse> =
                authRefreshService.refresh(req)

        fun forgotPassword(request: ForgotPasswordRequest) {
                val token = userService.generatePasswordResetToken(request.email)
                emailService.sendPasswordResetEmail(request.email, token)
        }

        fun resetPassword(request: ResetPasswordRequest) {
                userService.resetPassword(request.token, request.newPassword)
        }
}
