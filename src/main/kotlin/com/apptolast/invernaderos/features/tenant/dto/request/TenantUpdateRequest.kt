package com.apptolast.invernaderos.features.tenant.dto.request

import com.apptolast.invernaderos.features.tenant.LocationDto
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Solicitud para actualizar un Tenant existente")
data class TenantUpdateRequest(
    @Schema(description = "Nombre completo del cliente/empresa")
    val name: String? = null,

    @Schema(description = "Correo electrónico")
    val email: String? = null,

    @Schema(description = "Teléfono de contacto")
    val phone: String? = null,

    @Schema(description = "Provincia")
    val province: String? = null,

    @Schema(description = "País")
    val country: String? = null,

    @Schema(description = "Coordenadas geográficas")
    val location: LocationDto? = null,

    @Schema(description = "Estado (Activo, Pendiente, Inactivo)")
    val status: String? = null
)
