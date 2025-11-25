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
}
