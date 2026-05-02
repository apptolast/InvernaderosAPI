package com.apptolast.invernaderos.features.auth

import com.apptolast.invernaderos.features.auth.dto.request.ForgotPasswordRequest
import com.apptolast.invernaderos.features.auth.dto.request.ResetPasswordRequest
import com.apptolast.invernaderos.features.user.UserService
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test

class AuthServiceTest {

    private val userService: UserService = mockk(relaxed = true)
    private val emailService: EmailService = mockk(relaxed = true)
    private val authRefreshService: AuthRefreshService = mockk(relaxed = true)

    private val authService = AuthService(userService, emailService, authRefreshService)

    @Test
    fun `forgotPassword should generate token and send email`() {
        val email = "test@example.com"
        val token = "reset-token"
        val request = ForgotPasswordRequest(email)

        every { userService.generatePasswordResetToken(email) } returns token

        authService.forgotPassword(request)

        verify { userService.generatePasswordResetToken(email) }
        verify { emailService.sendPasswordResetEmail(email, token) }
    }

    @Test
    fun `resetPassword should call userService resetPassword`() {
        val token = "reset-token"
        val newPassword = "newPassword123"
        val request = ResetPasswordRequest(token, newPassword)

        authService.resetPassword(request)

        verify { userService.resetPassword(token, newPassword) }
    }
}
