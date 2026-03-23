package com.apptolast.invernaderos.features.tenant.dto.request

import com.apptolast.invernaderos.features.tenant.LocationDto
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Solicitud para crear un nuevo Tenant")
data class TenantCreateRequest(
    @Schema(description = "Nombre completo del cliente/empresa", example = "Elena Rodriguez")
    val name: String,

    @Schema(description = "Correo electrónico", example = "elena@freshveg.com")
    val email: String,

    @Schema(description = "Teléfono de contacto", example = "+34 612 345 678")
    val phone: String? = null,

    @Schema(description = "Provincia", example = "Almería")
    val province: String? = null,

    @Schema(description = "País", example = "España")
    val country: String? = "España",

    @Schema(description = "Coordenadas geográficas")
    val location: LocationDto? = null,

    @Schema(description = "Estado inicial (Activo, Pendiente, Inactivo)", example = "Activo")
    val status: String? = "Activo"
)
