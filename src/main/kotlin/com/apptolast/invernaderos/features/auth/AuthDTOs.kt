package com.apptolast.invernaderos.features.auth

import com.fasterxml.jackson.annotation.JsonProperty

data class LoginRequest(
        val username: String, // Can be username or email
        val password: String
)

data class RegisterRequest(
        @JsonProperty("company_name") val companyName: String,
        @JsonProperty("tax_id") val taxId: String,
        val email: String,
        val password: String,
        @JsonProperty("first_name") val firstName: String, // For contact person
        @JsonProperty("last_name") val lastName: String, // For contact person
        val phone: String? = null,
        val address: String? = null
)

data class JwtResponse(
        val token: String,
        val type: String = "Bearer",
        val username: String,
        val roles: List<String>
)
