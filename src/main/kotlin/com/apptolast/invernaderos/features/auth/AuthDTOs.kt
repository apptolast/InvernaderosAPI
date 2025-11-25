package com.apptolast.invernaderos.features.auth

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

@Schema(description = "Request object for user login")
data class LoginRequest(
        @field:NotBlank(message = "Username/Email is required")
        @Schema(description = "Username or email", example = "admin@company.com")
        val username: String,
        @field:NotBlank(message = "Password is required")
        @Schema(description = "User password", example = "password123")
        val password: String
)

@Schema(description = "Request object for user registration")
data class RegisterRequest(
        @field:NotBlank(message = "Company Name is required")
        @field:Size(
                min = 2,
                max = 100,
                message = "Company Name must be between 2 and 100 characters"
        )
        @JsonProperty("company_name")
        @Schema(description = "Name of the company/tenant", example = "Greenhouse Tech")
        val companyName: String,
        @field:NotBlank(message = "Tax ID is required")
        @JsonProperty("tax_id")
        @Schema(description = "Tax identification number", example = "B12345678")
        val taxId: String,
        @field:NotBlank(message = "Email is required")
        @field:Email(message = "Invalid email format")
        @Schema(description = "User email address", example = "admin@greenhousetech.com")
        val email: String,
        @field:NotBlank(message = "Password is required")
        @field:Size(min = 6, message = "Password must be at least 6 characters long")
        @Schema(description = "User password", example = "securePass123")
        val password: String,
        @field:NotBlank(message = "First Name is required")
        @JsonProperty("first_name")
        @Schema(description = "Contact person first name", example = "John")
        val firstName: String,
        @field:NotBlank(message = "Last Name is required")
        @JsonProperty("last_name")
        @Schema(description = "Contact person last name", example = "Doe")
        val lastName: String,
        @Schema(description = "Contact phone number", example = "+34600123456")
        val phone: String? = null,
        @Schema(description = "Physical address", example = "Calle Principal 123, Madrid")
        val address: String? = null
)

@Schema(description = "Response object containing JWT token")
data class JwtResponse(
        @Schema(description = "JWT Access Token") val token: String,
        @Schema(description = "Token type", example = "Bearer") val type: String = "Bearer",
        @Schema(description = "Username/Email of the authenticated user") val username: String,
        @Schema(description = "List of roles assigned to the user") val roles: List<String>
)
