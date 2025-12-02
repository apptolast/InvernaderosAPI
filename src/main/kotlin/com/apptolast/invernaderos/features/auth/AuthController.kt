package com.apptolast.invernaderos.features.auth

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/auth")
class AuthController(private val authService: AuthService) {

    @PostMapping("/login")
    @Operation(
            summary = "Authenticate user",
            description = "Login with username and password to get JWT token"
    )
    @ApiResponse(responseCode = "200", description = "Successfully authenticated")
    @ApiResponse(responseCode = "401", description = "Invalid credentials")
    fun login(@Valid @RequestBody request: LoginRequest): ResponseEntity<JwtResponse> {
        return ResponseEntity.ok(authService.login(request))
    }

    @PostMapping("/register")
    @Operation(
            summary = "Register new tenant",
            description = "Register a new company and admin user"
    )
    @ApiResponse(responseCode = "200", description = "Successfully registered")
    @ApiResponse(responseCode = "400", description = "Invalid input data")
    fun register(@Valid @RequestBody request: RegisterRequest): ResponseEntity<JwtResponse> {
        return ResponseEntity.ok(authService.register(request))
    }

    @PostMapping("/forgot-password")
    @Operation(
            summary = "Request password reset",
            description = "Generates a reset token and sends it via email"
    )
    @ApiResponse(responseCode = "200", description = "Email sent if user exists")
    fun forgotPassword(@Valid @RequestBody request: ForgotPasswordRequest): ResponseEntity<Void> {
        authService.forgotPassword(request)
        return ResponseEntity.ok().build()
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Reset password", description = "Resets the user password using the token")
    @ApiResponse(responseCode = "200", description = "Password successfully reset")
    @ApiResponse(responseCode = "400", description = "Invalid or expired token")
    fun resetPassword(@Valid @RequestBody request: ResetPasswordRequest): ResponseEntity<Void> {
        authService.resetPassword(request)
        return ResponseEntity.ok().build()
    }
}
