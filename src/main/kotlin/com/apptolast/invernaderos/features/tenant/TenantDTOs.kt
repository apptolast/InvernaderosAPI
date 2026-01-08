package com.apptolast.invernaderos.features.tenant

import io.swagger.v3.oas.annotations.media.Schema

/**
 * DTO para coordenadas geográficas.
 * Compatible con el cliente KMP que envía {lat: Double, lon: Double}
 */
@Schema(description = "Coordenadas geográficas")
data class LocationDto(
    @Schema(description = "Latitud", example = "36.8381")
    val lat: Double? = null,

    @Schema(description = "Longitud", example = "-2.4597")
    val lon: Double? = null
)

@Schema(description = "Objeto de respuesta que representa un Tenant (Cliente)")
data class TenantResponse(
    @Schema(description = "Identificador único del tenant") val id: Long,
    @Schema(description = "Nombre del tenant") val name: String,
    @Schema(description = "Email de contacto") val email: String,
    @Schema(description = "Teléfono de contacto") val phone: String?,
    @Schema(description = "Provincia") val province: String?,
    @Schema(description = "País") val country: String?,
    @Schema(description = "Coordenadas geográficas") val location: LocationDto?,
    @Schema(description = "Si el tenant está activo") val isActive: Boolean?,
    @Schema(description = "Estado para la UI (Activo, Pendiente, Inactivo)") val status: String
)

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

fun Tenant.toResponse(): TenantResponse {
    // Mapeo simple de isActive a status para la UI
    // Nota: Para soportar "Pendiente" se requeriría un cambio en la DB (Enum)
    val status = when (this.isActive) {
        true -> "Activo"
        false -> "Inactivo"
        null -> "Pendiente" // Mapeamos nulo a Pendiente como sugerencia
    }
    
    return TenantResponse(
        id = this.id ?: throw IllegalStateException("Tenant ID cannot be null"),
        name = this.name,
        email = this.email,
        phone = this.phone,
        province = this.province,
        country = this.country,
        location = this.location,
        isActive = this.isActive,
        status = status
    )
}
