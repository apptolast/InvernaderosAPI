package com.apptolast.invernaderos.features.auth

import com.apptolast.invernaderos.features.auth.dto.request.ForgotPasswordRequest
import com.apptolast.invernaderos.features.auth.dto.request.ResetPasswordRequest
import com.apptolast.invernaderos.features.user.UserService
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension

@ExtendWith(MockitoExtension::class)
class AuthServiceTest {

    @Mock lateinit var userService: UserService

    @Mock lateinit var emailService: EmailService

    @Mock lateinit var authRefreshService: AuthRefreshService

    @InjectMocks lateinit var authService: AuthService

    @Test
    fun `forgotPassword should generate token and send email`() {
        val email = "test@example.com"
        val token = "reset-token"
        val request = ForgotPasswordRequest(email)

        `when`(userService.generatePasswordResetToken(email)).thenReturn(token)

        authService.forgotPassword(request)

        verify(userService).generatePasswordResetToken(email)
        verify(emailService).sendPasswordResetEmail(email, token)
    }

    @Test
    fun `resetPassword should call userService resetPassword`() {
        val token = "reset-token"
        val newPassword = "newPassword123"
        val request = ResetPasswordRequest(token, newPassword)

        authService.resetPassword(request)

        verify(userService).resetPassword(token, newPassword)
    }
}
